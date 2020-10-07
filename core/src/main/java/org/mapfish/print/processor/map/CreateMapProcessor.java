package org.mapfish.print.processor.map;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import net.sf.jasperreports.engine.JRException;
import org.apache.batik.svggen.DefaultStyleHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.ImageUtils;
import org.mapfish.print.SvgUtil;
import org.mapfish.print.attribute.map.AreaOfInterest;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.GenericMapAttribute.GenericMapAttributeValues;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapLayer.RenderType;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.attribute.map.ZoomLevelSnapStrategy;
import org.mapfish.print.attribute.map.ZoomToFeatures.ZoomType;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.HttpRequestFetcher;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.Scale;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.map.geotools.FeatureLayer;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InputOutputValue;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.mapfish.print.processor.jasper.ImagesSubReport;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.geotools.renderer.lite.RendererUtilities.worldToScreenTransform;
import static org.mapfish.print.Constants.PDF_DPI;


/**
 * <p>A processor to render a map that can be embedded as a sub-report into a JasperReports
 * template.</p>
 * <p>See also: <a href="attributes.html#!map">!map</a> attribute</p>
 * [[examples=verboseExample]]
 */
public final class CreateMapProcessor
        extends AbstractProcessor<CreateMapProcessor.Input, CreateMapProcessor.Output> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateMapProcessor.class);

    @Autowired
    FeatureLayer.Plugin featureLayerPlugin;

    @Autowired
    private MetricRegistry metricRegistry;

    @Resource(name = "requestForkJoinPool")
    private ForkJoinPool requestForkJoinPool;

    /**
     * Constructor.
     */
    protected CreateMapProcessor() {
        super(Output.class);
    }

    private static RenderType getSupportedRenderType(final RenderType renderType) {
        if (renderType == RenderType.UNKNOWN || renderType == RenderType.TIFF) {
            return RenderType.PNG;
        } else {
            return renderType;
        }
    }

    /**
     * @param mapValues The map parameters.
     * @return The map context.
     */
    @VisibleForTesting
    public static MapfishMapContext createMapContext(final MapAttributeValues mapValues) {
        final Dimension mapSize = mapValues.getMapSize();
        Rectangle paintArea = new Rectangle(mapSize);

        final double dpi = mapValues.getDpi();

        MapBounds bounds = mapValues.getMapBounds();
        bounds = adjustBoundsToScaleAndMapSize(mapValues, paintArea, bounds, dpi);

        // if the DPI is higher than the PDF DPI we need to make the image larger so the image put in the
        // PDF is large enough for the
        // higher DPI printer
        final double dpiRatio = dpi / PDF_DPI;
        paintArea.setBounds(0, 0,
                            (int) Math.ceil(mapSize.getWidth() * dpiRatio),
                            (int) Math.ceil(mapSize.getHeight() * dpiRatio));

        return new MapfishMapContext(bounds, paintArea.getSize(), mapValues.getRotation(), dpi,
                                     mapValues.longitudeFirst, mapValues.isDpiSensitiveStyle());
    }

    /**
     * If requested, adjust the bounds to the nearest scale and the map size.
     *
     * @param mapValues Map parameters.
     * @param paintArea The size of the painting area.
     * @param bounds The map bounds.
     * @param dpi the DPI.
     */
    public static MapBounds adjustBoundsToScaleAndMapSize(
            final GenericMapAttributeValues mapValues,
            final Rectangle paintArea,
            final MapBounds bounds,
            final double dpi) {
        MapBounds newBounds = bounds;
        if (mapValues.isUseNearestScale()) {
            newBounds = newBounds.adjustBoundsToNearestScale(
                    mapValues.getZoomLevels(),
                    mapValues.getZoomSnapTolerance(),
                    mapValues.getZoomLevelSnapStrategy(),
                    mapValues.getZoomSnapGeodetic(),
                    paintArea, dpi);
        }

        newBounds = new BBoxMapBounds(newBounds.toReferencedEnvelope(paintArea));

        if (mapValues.isUseAdjustBounds()) {
            newBounds = newBounds.adjustedEnvelope(paintArea);
        }
        return newBounds;
    }

    /**
     * Create a SVG graphic with the give dimensions.
     *
     * @param size The size of the SVG graphic.
     */
    public static SVGGraphics2D createSvgGraphics(final Dimension size)
            throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.getDOMImplementation().createDocument(null, "svg", null);

        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
        ctx.setStyleHandler(new OpacityAdjustingStyleHandler());
        ctx.setComment("Generated by GeoTools2 with Batik SVG Generator");

        SVGGraphics2D g2d = new SVGGraphics2D(ctx, true);
        g2d.setSVGCanvasSize(size);

        return g2d;
    }

    /**
     * Save a SVG graphic to the given path.
     *
     * @param graphics2d The SVG graphic to save.
     * @param path The file.
     */
    public static void saveSvgFile(final SVGGraphics2D graphics2d, final File path) throws IOException {
        try (FileOutputStream fs = new FileOutputStream(path);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fs, StandardCharsets.UTF_8);
             Writer osw = new BufferedWriter(outputStreamWriter)
        ) {
            graphics2d.stream(osw, true);
        }
    }

    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Override
    public Output execute(final Input param, final ExecutionContext context)
            throws IOException, ParserConfigurationException, JRException {
        context.stopIfCanceled();
        MapAttributeValues mapValues = (MapAttributeValues) param.map;
        if (mapValues.zoomToFeatures != null) {
            zoomToFeatures(param.clientHttpRequestFactoryProvider.get(), mapValues, context);
        }
        final MapfishMapContext mapContext = createMapContext(mapValues);
        boolean pdfA = param.template.isPdfA();
        if (mapValues.pdfA != null) {
            pdfA = mapValues.pdfA;
        }
        final List<URI> graphics = createLayerGraphics(
                param.tempTaskDirectory,
                param.clientHttpRequestFactoryProvider.get(),
                pdfA,
                mapValues, context, mapContext);
        context.stopIfCanceled();

        final URI mapSubReport;
        if (param.map.getTemplate().isMapExport()) {
            mapSubReport =
                    createMergedGraphic(param.tempTaskDirectory, graphics, mapContext, param.outputFormat);
        } else {
            mapSubReport = createMapSubReport(param.tempTaskDirectory, mapValues.getMapSize(), graphics,
                                              mapValues.getDpi());
        }

        context.getStats().addMapStats(mapContext, mapValues);

        return new Output(graphics, mapSubReport.toString(), mapContext);
    }

    @Override
    protected void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
    }

    private URI createMergedGraphic(
            final File printDirectory,
            final List<URI> graphics,
            final MapfishMapContext mapContext,
            final String outputFormat) throws IOException {

        final File mergedGraphic = File.createTempFile("map-", "." + outputFormat, printDirectory);
        int width = mapContext.getMapSize().width;
        int height = mapContext.getMapSize().height;

        if ("pdf".equalsIgnoreCase(outputFormat)) {
            final com.lowagie.text.Document document = new com.lowagie.text.Document(
                    new com.lowagie.text.Rectangle(width, height));
            try {
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(mergedGraphic));
                document.open();
                PdfContentByte pdfCB = writer.getDirectContent();
                Graphics g = pdfCB.createGraphics(width, height);
                try {
                    drawGraphics(width, height, graphics, g);
                } finally {
                    g.dispose();
                }
            } catch (DocumentException e) {
                throw new IOException(e);
            } finally {
                document.close();
            }
        } else {
            boolean isJpeg = RenderType.fromFileExtension(outputFormat) == RenderType.JPEG;
            final BufferedImage bufferedImage = new BufferedImage(
                    width, height, isJpeg ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_4BYTE_ABGR);
            Graphics g = bufferedImage.getGraphics();
            if (isJpeg) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
            }
            try {
                drawGraphics(width, height, graphics, g);
            } finally {
                g.dispose();
            }
            ImageUtils.writeImage(bufferedImage, outputFormat, mergedGraphic);
        }

        return mergedGraphic.toURI();
    }

    private void drawGraphics(
            final int width, final int height,
            final List<URI> graphics, final Graphics g) throws IOException {
        for (URI graphic: graphics) {
            final File graphicFile = new File(graphic);
            if (FilenameUtils.getExtension(graphicFile.getName()).equals("svg")) {
                try {
                    g.drawImage(SvgUtil.convertFromSvg(graphic, width, height), 0, 0, width, height, null);
                } catch (TranscoderException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                BufferedImage image = ImageIO.read(graphicFile);
                g.drawImage(image, 0, 0, width, height, null);
            }
        }
    }

    private URI createMapSubReport(
            final File printDirectory,
            final Dimension mapSize,
            final List<URI> graphics,
            final double dpi) throws IOException, JRException {
        final ImagesSubReport subReport = new ImagesSubReport(graphics, mapSize, dpi);

        final File compiledReport = File.createTempFile("map-",
                                                        JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT,
                                                        printDirectory);
        subReport.compile(compiledReport);

        return compiledReport.toURI();
    }

    private void warnIfDifferentRenderType(
            final RenderType renderType, final MapLayer layer, final boolean allowTransparency) {
        if (allowTransparency && renderType != layer.getRenderType() &&
                layer.getRenderType() != RenderType.UNKNOWN) {
            LOGGER.info("Layer {} has {} format, storing as PNG.",
                        layer.getName().isEmpty() ? layer : layer.getName(),
                        layer.getRenderType().toString());
        }
    }

    private MapfishMapContext getTransformer(
            final MapfishMapContext mapContext, final double imageBufferScaling) {
        return new MapfishMapContext(
                mapContext,
                mapContext.getBounds(),
                new Dimension(
                        (int) Math.round(mapContext.getMapSize().width * imageBufferScaling),
                        (int) Math.round(mapContext.getMapSize().height * imageBufferScaling)
                ),
                mapContext.getRotation(),
                mapContext.getDPI(),
                mapContext.isForceLongitudeFirst(),
                mapContext.isDpiSensitiveStyle()
        );
    }

    private List<URI> createLayerGraphics(
            final File printDirectory,
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final boolean pdfA,
            final MapAttributeValues mapValues,
            final ExecutionContext context,
            final MapfishMapContext mapContext) throws IOException, ParserConfigurationException {
        // reverse layer list to draw from bottom to top.  normally position 0 is top-most layer.
        final List<MapLayer> layers = new ArrayList<>(mapValues.getLayers());
        Collections.reverse(layers);

        final AreaOfInterest areaOfInterest = addAreaOfInterestLayer(mapValues, layers);

        final String mapKey = UUID.randomUUID().toString();
        final List<URI> graphics = new ArrayList<>(layers.size());

        HttpRequestFetcher cache = new HttpRequestFetcher(printDirectory, this.metricRegistry,
                                                          context, this.requestForkJoinPool);

        //prepare layers for rendering
        for (final MapLayer layer: layers) {
            layer.prepareRender(mapContext);
            final MapfishMapContext transformer = getTransformer(mapContext,
                                                                 layer.getImageBufferScaling());
            layer.prefetchResources(cache, clientHttpRequestFactory, transformer, context);
        }

        final Timer.Context timer =
                this.metricRegistry.timer(getClass().getName() + ".buildLayers").time();
        int fileNumber = 0;
        for (LayerGroup layerGroup: LayerGroup.buildGroups(layers, pdfA)) {
            if (layerGroup.renderType == RenderType.SVG) {
                // render layers as SVG
                for (MapLayer layer: layerGroup.layers) {
                    context.stopIfCanceled();
                    final SVGGraphics2D graphics2D = createSvgGraphics(mapContext.getMapSize());

                    try {
                        final Graphics2D clippedGraphics2D = createClippedGraphics(
                                mapContext, areaOfInterest, graphics2D);
                        layer.render(clippedGraphics2D, clientHttpRequestFactory, mapContext, context);

                        final File path =
                                new File(printDirectory, mapKey + "_layer_" + fileNumber++ + ".svg");
                        saveSvgFile(graphics2D, path);
                        graphics.add(path.toURI());
                    } finally {
                        graphics2D.dispose();
                    }
                }
            } else {
                // render layers as raster graphic
                final boolean needTransparency = !layerGroup.opaque && !pdfA;
                final BufferedImage bufferedImage = new BufferedImage(
                        (int) Math.round(mapContext.getMapSize().width * layerGroup.imageBufferScaling),
                        (int) Math.round(mapContext.getMapSize().height * layerGroup.imageBufferScaling),
                        needTransparency ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR
                );
                final Graphics2D graphics2D = createClippedGraphics(
                        mapContext, areaOfInterest,
                        bufferedImage.createGraphics()
                );
                try {
                    if (layerGroup.opaque || pdfA) {
                        // the image is opaque and therefore needs a white background
                        final Color prevColor = graphics2D.getColor();
                        graphics2D.setColor(Color.WHITE);
                        graphics2D.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
                        graphics2D.setColor(prevColor);
                    }

                    final MapfishMapContext transformer =
                            getTransformer(mapContext, layerGroup.imageBufferScaling);
                    for (MapLayer cur: layerGroup.layers) {
                        context.stopIfCanceled();
                        warnIfDifferentRenderType(layerGroup.renderType, cur, !pdfA);
                        cur.render(graphics2D, clientHttpRequestFactory, transformer, context);
                    }

                    // Try to respect the original format of the layer. But if it needs to be transparent,
                    // no choice, we need PNG.
                    final String formatName =
                            layerGroup.opaque && layerGroup.renderType == RenderType.JPEG ? "JPEG" : "PNG";
                    final File path = new File(printDirectory,
                                               String.format("%s_layer_%d.%s", mapKey, fileNumber++,
                                                             formatName.toLowerCase()));
                    ImageUtils.writeImage(bufferedImage, formatName, path);
                    graphics.add(path.toURI());
                } finally {
                    graphics2D.dispose();
                }
            }
        }
        timer.stop();

        return graphics;
    }

    private AreaOfInterest addAreaOfInterestLayer(
            @Nonnull final MapAttributeValues mapValues,
            @Nonnull final List<MapLayer> layers) {
        final AreaOfInterest areaOfInterest = mapValues.areaOfInterest;
        if (areaOfInterest != null && areaOfInterest.display == AreaOfInterest.AoiDisplay.RENDER) {
            FeatureLayer.FeatureLayerParam param = new FeatureLayer.FeatureLayerParam();
            param.defaultStyle = Constants.Style.OverviewMap.NAME + ":" +
                    areaOfInterest.getArea().getClass().getSimpleName();

            param.style = areaOfInterest.style;
            param.renderAsSvg = areaOfInterest.renderAsSvg;
            param.features = areaOfInterest.areaToFeatureCollection(mapValues);
            final FeatureLayer featureLayer = this.featureLayerPlugin.parse(mapValues.getTemplate(), param);

            layers.add(featureLayer);
        }
        return areaOfInterest;
    }

    private Graphics2D createClippedGraphics(
            @Nonnull final MapfishMapContext transformer,
            @Nullable final AreaOfInterest areaOfInterest,
            @Nonnull final Graphics2D graphics2D
    ) {
        if (areaOfInterest != null && areaOfInterest.display == AreaOfInterest.AoiDisplay.CLIP) {
            final Polygon screenGeometry = areaOfInterestInScreenCRS(transformer, areaOfInterest);
            final ShapeWriter shapeWriter = new ShapeWriter();
            final Shape clipShape = shapeWriter.toShape(screenGeometry);
            return new ConstantClipGraphics2D(graphics2D, clipShape);
        }

        return graphics2D;
    }

    private Polygon areaOfInterestInScreenCRS(
            @Nonnull final MapfishMapContext transformer,
            @Nullable final AreaOfInterest areaOfInterest) {
        if (areaOfInterest != null) {
            final AffineTransform worldToScreenTransform = worldToScreenTransform(
                    transformer.toReferencedEnvelope(),
                    transformer.getPaintArea()
            );

            MathTransform mathTransform = new AffineTransform2D(worldToScreenTransform);
            final Polygon screenGeometry;
            try {
                screenGeometry = (Polygon) JTS.transform(areaOfInterest.getArea(), mathTransform);
            } catch (TransformException e) {
                throw new RuntimeException(e);
            }
            return screenGeometry;
        }

        return null;
    }

    private void zoomToFeatures(
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final MapAttributeValues mapValues,
            final ExecutionContext context) {
        ReferencedEnvelope bounds = getFeatureBounds(clientHttpRequestFactory,
                                                     mapValues, context);

        if (!bounds.isNull()) {
            if (mapValues.zoomToFeatures.zoomType == ZoomType.CENTER) {
                // center the map on the center of the feature bounds
                Coordinate center = bounds.centre();
                mapValues.center = new double[]{center.x, center.y};
                if (mapValues.zoomToFeatures.minScale != null) {
                    LOGGER.warn(
                            "The map.zoomToFeatures.minScale is deprecated, please use directly the map" +
                                    ".scale");
                    mapValues.scale = mapValues.zoomToFeatures.minScale;
                }
                mapValues.recalculateBounds();
            } else if (mapValues.zoomToFeatures.zoomType == ZoomType.EXTENT) {
                if (bounds.getWidth() == 0.0 && bounds.getHeight() == 0.0) {
                    // single point, so we directly set the center
                    Coordinate center = bounds.centre();
                    mapValues.center = new double[]{center.x, center.y};
                    mapValues.bbox = null;
                    mapValues.scale = mapValues.zoomToFeatures.minScale;
                    mapValues.recalculateBounds();
                } else {
                    mapValues.bbox = new double[]{
                            bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()
                    };
                    mapValues.center = null;
                    mapValues.recalculateBounds();

                    MapBounds mapBounds = mapValues.getMapBounds();
                    Rectangle paintArea = new Rectangle(mapValues.getMapSize());

                    if (mapValues.zoomToFeatures.minMargin != null) {
                        // add a margin around the feature bounds
                        mapBounds = new BBoxMapBounds(mapBounds.toReferencedEnvelope(paintArea)).expand(
                                mapValues.zoomToFeatures.minMargin, paintArea);
                    }

                    // expand the bounds so that they match the ratio of the paint area
                    mapBounds = mapBounds.adjustedEnvelope(paintArea);

                    final Scale scale = mapBounds.getScale(paintArea, mapValues.getDpi());
                    final Scale minScale = new Scale(mapValues.zoomToFeatures.minScale,
                                                     mapValues.getMapBounds().getProjection(),
                                                     mapValues.getDpi());
                    if (scale.getResolution() < minScale.getResolution()) {
                        // if the current scale is smaller than the min. scale, change it
                        mapBounds = mapBounds.zoomToScale(minScale);
                    }

                    if (mapValues.isUseNearestScale()) {
                        // if fixed scales are used, take next higher scale
                        mapBounds = mapBounds.adjustBoundsToNearestScale(
                                mapValues.getZoomLevels(), 0.0,
                                ZoomLevelSnapStrategy.HIGHER_SCALE,
                                mapValues.getZoomSnapGeodetic(),
                                paintArea, mapValues.getDpi());
                    }

                    mapValues.setMapBounds(mapBounds);
                }
            }
        } else {
            LOGGER.warn("No BBOX found for zoomToFeature");
        }
    }

    /**
     * Get the bounding-box containing all features of all layers.
     */
    @Nonnull
    private ReferencedEnvelope getFeatureBounds(
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final MapAttributeValues mapValues, final ExecutionContext context) {
        final MapfishMapContext mapContext = createMapContext(mapValues);

        String layerName = mapValues.zoomToFeatures.layer;
        ReferencedEnvelope bounds = new ReferencedEnvelope();
        for (MapLayer layer: mapValues.getLayers()) {
            context.stopIfCanceled();

            if ((!StringUtils.isEmpty(layerName) && layerName.equals(layer.getName())) ||
                    (StringUtils.isEmpty(layerName) && layer instanceof AbstractFeatureSourceLayer)) {
                AbstractFeatureSourceLayer featureLayer = (AbstractFeatureSourceLayer) layer;
                FeatureSource<?, ?> featureSource =
                        featureLayer.getFeatureSource(clientHttpRequestFactory, mapContext);
                FeatureCollection<?, ?> features;
                try {
                    features = featureSource.getFeatures();
                } catch (IOException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }

                if (!features.isEmpty()) {
                    final ReferencedEnvelope curBounds = features.getBounds();
                    bounds.expandToInclude(curBounds);
                }
            }
        }

        return bounds;
    }

    /**
     * The Input object for processor.
     */
    public static class Input {
        /**
         * A factory for making http requests.  This is added to the values by the framework and therefore
         * does not need to be set in configuration
         */
        public MfClientHttpRequestFactoryProvider clientHttpRequestFactoryProvider;

        /**
         * The required parameters for the map.
         */
        // e.g. the grid layer will be self modified on drawing, and we need the result for the overview map.
        @InputOutputValue
        public GenericMapAttributeValues map;

        /**
         * The path to the temporary directory for the print task.
         */
        public File tempTaskDirectory;

        /**
         * The template containing this table processor.
         */
        public Template template;

        /**
         * The output format.
         */
        @HasDefaultValue
        public String outputFormat = null;
    }

    /**
     * Output for the processor.
     */
    public static final class Output {

        /**
         * The paths to a graphic for each layer.
         */
        @InternalValue
        public final List<URI> layerGraphics;

        /**
         * The path to the compiled sub-report for the map.
         */
        public final String mapSubReport;

        /**
         * The map parameters used after zooming and all other calculations that are made.
         */
        public final MapfishMapContext mapContext;

        private Output(
                final List<URI> layerGraphics,
                final String mapSubReport,
                final MapfishMapContext mapContext) {
            this.layerGraphics = layerGraphics;
            this.mapSubReport = mapSubReport;
            this.mapContext = mapContext;
        }
    }

    private static final class OpacityAdjustingStyleHandler extends DefaultStyleHandler {
        @Override
        public void setStyle(
                final Element element,
                final Map styleMap,
                final SVGGeneratorContext generatorContext) {
            String tagName = element.getTagName();
            for (final Object o: styleMap.keySet()) {
                String styleName = (String) o;
                if (element.getAttributeNS(null, styleName).length() == 0) {
                    if (styleName.equals("opacity")) {
                        if (appliesTo(styleName, tagName)) {
                            element.setAttributeNS(null, "fill-opacity",
                                                   (String) styleMap.get(styleName));
                            element.setAttributeNS(null, "stroke-opacity",
                                                   (String) styleMap.get(styleName));
                        }
                    } else {
                        if (appliesTo(styleName, tagName)) {
                            element.setAttributeNS(null, styleName,
                                                   (String) styleMap.get(styleName));
                        }
                    }
                }
            }
        }
    }

    /**
     * Class that groups together layers that can end up in the same file.
     */
    private static final class LayerGroup {
        public final List<MapLayer> layers = new ArrayList<>();
        public final RenderType renderType;
        public final double imageBufferScaling;
        public boolean opaque = false;

        private LayerGroup(final RenderType renderType, final double imageBufferScaling) {
            this.renderType = renderType;
            this.imageBufferScaling = imageBufferScaling;
        }

        public static List<LayerGroup> buildGroups(
                final List<MapLayer> layers, final boolean mergeAsJPEGWithScale1) {
            final List<LayerGroup> result = new ArrayList<>();
            if (!mergeAsJPEGWithScale1) {
                LOGGER.debug("Building groups of layers");
                for (int i = 0; i < layers.size(); ) {
                    final RenderType renderType = getSupportedRenderType(layers.get(i).getRenderType());
                    final double imageBufferScaling = layers.get(i).getImageBufferScaling();
                    final LayerGroup group = new LayerGroup(renderType, imageBufferScaling);
                    MapLayer l = layers.get(i);
                    LOGGER.debug("New group for layer {}, {}", l.getRenderType(), l.getImageBufferScaling());
                    group.opaque = (i == 0 && renderType == RenderType.JPEG);

                    // Merge consecutive layers of same render type and same buffer scaling (native
                    // resolution)
                    while (i < layers.size() &&
                        getSupportedRenderType(layers.get(i).getRenderType()) == renderType &&
                            imageBufferScaling == layers.get(i).getImageBufferScaling()) {
                        // will always go there the first time
                        l = layers.get(i);
                        LOGGER.debug("Adding layer {} to the group", l.getName());
                        group.layers.add(l);
                        group.opaque = group.opaque ||
                                (renderType == RenderType.JPEG && l.getOpacity() == 1.0);
                        ++i;
                    }

                    result.add(group);
                }
            } else {
                LOGGER.debug("Merging together all layers as a JPEG image of scaling 1");
                final LayerGroup group = new LayerGroup(RenderType.JPEG, 1.0);
                group.opaque = true;
                group.layers.addAll(layers);
                result.add(group);
            }

            return result;
        }
    }
}
