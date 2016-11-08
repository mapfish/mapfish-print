package org.mapfish.print.processor.map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

import net.sf.jasperreports.engine.JRException;

import org.apache.batik.svggen.DefaultStyleHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.AreaOfInterest;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.attribute.map.ZoomLevelSnapStrategy;
import org.mapfish.print.attribute.map.ZoomToFeatures.ZoomType;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.map.geotools.FeatureLayer;
import org.mapfish.print.map.geotools.grid.GridLayer;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.jasper.ImagesSubReport;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.Dimension;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.geotools.renderer.lite.RendererUtilities.worldToScreenTransform;


/**
 * <p>A processor to render a map that can be embedded as a sub-report into a JasperReports
 * template.</p>
 * <p>See also: <a href="attributes.html#!map">!map</a> attribute</p>
 * [[examples=verboseExample]]
 */
public final class CreateMapProcessor extends AbstractProcessor<CreateMapProcessor.Input, CreateMapProcessor.Output> {
    enum BufferedImageType {
        TYPE_4BYTE_ABGR(BufferedImage.TYPE_4BYTE_ABGR),
        TYPE_4BYTE_ABGR_PRE(BufferedImage.TYPE_4BYTE_ABGR_PRE),
        TYPE_3BYTE_BGR(BufferedImage.TYPE_3BYTE_BGR),
        TYPE_BYTE_BINARY(BufferedImage.TYPE_BYTE_BINARY),
        TYPE_BYTE_GRAY(BufferedImage.TYPE_BYTE_GRAY),
        TYPE_BYTE_INDEXED(BufferedImage.TYPE_BYTE_INDEXED),
        TYPE_INT_BGR(BufferedImage.TYPE_INT_BGR),
        TYPE_INT_RGB(BufferedImage.TYPE_INT_RGB),
        TYPE_INT_ARGB(BufferedImage.TYPE_INT_ARGB),
        TYPE_INT_ARGB_PRE(BufferedImage.TYPE_INT_ARGB_PRE),
        TYPE_USHORT_555_RGB(BufferedImage.TYPE_USHORT_555_RGB),
        TYPE_USHORT_565_RGB(BufferedImage.TYPE_USHORT_565_RGB),
        TYPE_USHORT_GRAY(BufferedImage.TYPE_USHORT_GRAY);
        private final int value;

        private BufferedImageType(final int value) {
            this.value = value;
        }

        static BufferedImageType lookupValue(final String name) {
            for (BufferedImageType bufferedImageType : values()) {
                if (bufferedImageType.name().equalsIgnoreCase(name)) {
                    return bufferedImageType;
                }
            }

            throw new IllegalArgumentException("'" + name + "is not a recognized " + BufferedImageType.class.getName() + " enum value");

        }
    }

    @Autowired
    FeatureLayer.Plugin featureLayerPlugin;

    private BufferedImageType imageType = BufferedImageType.TYPE_4BYTE_ABGR;

    /**
     * Constructor.
     */
    protected CreateMapProcessor() {
        super(Output.class);
    }

    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Override
    public Output execute(final Input param, final ExecutionContext context) throws Exception {
        checkCancelState(context);
        MapAttribute.MapAttributeValues mapValues = param.map;
        if (mapValues.zoomToFeatures != null) {
            zoomToFeatures(param.tempTaskDirectory, param.clientHttpRequestFactory, mapValues, context);
        }
        final MapfishMapContext mapContext = createMapContext(mapValues);
        final List<URI> graphics = createLayerGraphics(param.tempTaskDirectory, param.clientHttpRequestFactory,
                mapValues, context, mapContext);
        checkCancelState(context);
        final URI mapSubReport = createMapSubReport(param.tempTaskDirectory, mapValues.getMapSize(), graphics, mapValues.getDpi());

        return new Output(graphics, mapSubReport.toString(), mapContext);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.imageType == null) {
            validationErrors.add(new ConfigurationException("No imageType defined in " + getClass().getName()));
        }
    }

    private URI createMapSubReport(final File printDirectory,
                                   final Dimension mapSize,
                                   final List<URI> graphics,
                                   final double dpi) throws IOException, JRException {
        final ImagesSubReport subReport = new ImagesSubReport(graphics, mapSize, dpi);

        final File compiledReport = File.createTempFile("map-",
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, printDirectory);
        subReport.compile(compiledReport);

        return compiledReport.toURI();
    }

    private List<URI> createLayerGraphics(final File printDirectory,
                                          final MfClientHttpRequestFactory clientHttpRequestFactory,
                                          final MapAttribute.MapAttributeValues mapValues,
                                          final ExecutionContext context,
                                          final MapfishMapContext mapContext)
            throws Exception {
        // reverse layer list to draw from bottom to top.  normally position 0 is top-most layer.
        final List<MapLayer> layers = Lists.reverse(Lists.newArrayList(mapValues.getLayers()));

        final AreaOfInterest areaOfInterest = addAreaOfInterestLayer(mapValues, layers);

        final String mapKey = UUID.randomUUID().toString();
        final List<URI> graphics = new ArrayList<URI>(layers.size());
        int i = 0;
        for (MapLayer layer : layers) {
            checkCancelState(context);
            File path = null;
            layer.prepareRender(mapContext);
            if (renderAsSvg(layer)) {
                // render layer as SVG
                final SVGGraphics2D graphics2D = getSvgGraphics(mapContext.getMapSize());

                try {
                    Graphics2D clippedGraphics2D = createClippedGraphics(mapContext, areaOfInterest, graphics2D);
                    layer.render(clippedGraphics2D, clientHttpRequestFactory, mapContext);

                    path = new File(printDirectory, mapKey + "_layer_" + i + ".svg");
                    saveSvgFile(graphics2D, path);
                } finally {
                    graphics2D.dispose();
                }
            } else {
                // render layer as raster graphic
                final double imageBufferScaling = layer.getImageBufferScaling();
                final BufferedImage bufferedImage = new BufferedImage(
                        (int) Math.round(mapContext.getMapSize().width * imageBufferScaling),
                        (int) Math.round(mapContext.getMapSize().height * imageBufferScaling),
                        this.imageType.value
                );
                Graphics2D graphics2D = createClippedGraphics(
                        mapContext, areaOfInterest,
                        bufferedImage.createGraphics()
                );
                try {
                    MapfishMapContext transformer = new MapfishMapContext(
                            mapContext,
                            mapContext.getBounds(),
                            new Dimension(
                                    (int) Math.round(mapContext.getMapSize().width * imageBufferScaling),
                                    (int) Math.round(mapContext.getMapSize().height * imageBufferScaling)
                            ),
                            mapContext.getRotation(), false,
                            mapContext.getDPI(),
                            mapContext.getRequestorDPI(),
                            mapContext.isForceLongitudeFirst(),
                            mapContext.isDpiSensitiveStyle()
                    );
                    layer.render(graphics2D, clientHttpRequestFactory, transformer);

                    path = new File(printDirectory, mapKey + "_layer_" + i + ".png");
                    ImageIO.write(bufferedImage, "png", path);
                } finally {
                    graphics2D.dispose();
                }
            }
            graphics.add(path.toURI());
            i++;
        }

        return graphics;
    }

    /**
     * @param mapValues The map parameters.
     * @return The map context.
     */
    @VisibleForTesting
    public static MapfishMapContext createMapContext(final MapAttribute.MapAttributeValues mapValues) {
        final Dimension mapSize = mapValues.getMapSize();
        Rectangle paintArea = new Rectangle(mapSize);

        final double dpi = mapValues.getDpi();
        final double dpiOfRequestor = mapValues.getRequestorDPI();

        // if the DPI is higher than the PDF DPI we need to make the image larger so the image put in the PDF is large enough for the
        // higher DPI printer
        final double dpiRatio = dpi / dpiOfRequestor;
        paintArea.setBounds(0, 0, (int) (mapSize.getWidth() * dpiRatio), (int) (mapSize.getHeight() * dpiRatio));

        MapBounds bounds = mapValues.getMapBounds();
        bounds = adjustBoundsToScaleAndMapSize(mapValues, dpi, paintArea, bounds);

        return new MapfishMapContext(bounds, paintArea.getSize(),
                mapValues.getRotation(), dpi, mapValues.getRequestorDPI(), mapValues.longitudeFirst, mapValues.isDpiSensitiveStyle());
    }


    private AreaOfInterest addAreaOfInterestLayer(@Nonnull final MapAttribute.MapAttributeValues mapValues,
                                                  @Nonnull final List<MapLayer> layers) throws IOException {
        final AreaOfInterest areaOfInterest = mapValues.areaOfInterest;
        if (areaOfInterest != null && areaOfInterest.display == AreaOfInterest.AoiDisplay.RENDER) {
            FeatureLayer.FeatureLayerParam param = new FeatureLayer.FeatureLayerParam();
            param.defaultStyle = Constants.Style.OverviewMap.NAME + ":" + areaOfInterest.getArea().getClass().getSimpleName();

            param.style = areaOfInterest.style;
            param.renderAsSvg = areaOfInterest.renderAsSvg;
            param.features = areaOfInterest.areaToFeatureCollection(mapValues);
            final FeatureLayer featureLayer = this.featureLayerPlugin.parse(mapValues.getTemplate(), param);

            layers.add(featureLayer);
        }
        return areaOfInterest;
    }

    private Graphics2D createClippedGraphics(@Nonnull final MapfishMapContext transformer,
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

    private Polygon areaOfInterestInScreenCRS(@Nonnull final MapfishMapContext transformer,
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

    /**
     * If requested, adjust the bounds to the nearest scale and the map size.
     *
     * @param mapValues      Map parameters
     * @param dpiOfRequestor The DPI.
     * @param paintArea      The size of the painting area.
     * @param bounds         The map bounds.
     */
    public static MapBounds adjustBoundsToScaleAndMapSize(
            final MapAttribute.MapAttributeValues mapValues, final double dpiOfRequestor,
            final Rectangle paintArea, final MapBounds bounds) {
        MapBounds newBounds = bounds;
        if (mapValues.isUseNearestScale()) {
            newBounds = newBounds.adjustBoundsToNearestScale(
                    mapValues.getZoomLevels(),
                    mapValues.getZoomSnapTolerance(),
                    mapValues.getZoomLevelSnapStrategy(),
                    mapValues.getZoomSnapGeodetic(),
                    paintArea, dpiOfRequestor);
        }

        newBounds = new BBoxMapBounds(newBounds.toReferencedEnvelope(paintArea, dpiOfRequestor));

        if (mapValues.isUseAdjustBounds()) {
            newBounds = newBounds.adjustedEnvelope(paintArea);
        }
        return newBounds;
    }

    private boolean renderAsSvg(final MapLayer layer) {
        if (layer instanceof AbstractFeatureSourceLayer) {
            AbstractFeatureSourceLayer featureLayer = (AbstractFeatureSourceLayer) layer;
            return featureLayer.shouldRenderAsSvg();
        }
        return false;
    }

    /**
     * Create a SVG graphic with the give dimensions.
     *
     * @param size The size of the SVG graphic.
     */
    public static SVGGraphics2D getSvgGraphics(final Dimension size)
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
     * @param path       The file.
     */
    public static void saveSvgFile(final SVGGraphics2D graphics2d, final File path) throws IOException {
        Closer closer = Closer.create();
        try {
            final FileOutputStream fs = closer.register(new FileOutputStream(path));
            final OutputStreamWriter outputStreamWriter = closer.register(new OutputStreamWriter(fs, "UTF-8"));
            Writer osw = closer.register(new BufferedWriter(outputStreamWriter));

            graphics2d.stream(osw, true);
        } finally {
            closer.close();
        }
    }

    private void zoomToFeatures(final File tempTaskDirectory, final MfClientHttpRequestFactory clientHttpRequestFactory,
            final MapAttributeValues mapValues, final ExecutionContext context) {

        ReferencedEnvelope bounds = getFeatureBounds(clientHttpRequestFactory,
                mapValues, context);

        if (bounds != null) {
            if (mapValues.zoomToFeatures.zoomType == ZoomType.CENTER) {
                // center the map on the center of the feature bounds
                Coordinate center = bounds.centre();
                mapValues.center = new double[] {center.x, center.y};
                if (mapValues.zoomToFeatures.minScale != null) {
                    mapValues.scale = mapValues.zoomToFeatures.minScale;
                }
                mapValues.bbox = null;
                mapValues.recalculateBounds();
            } else if (mapValues.zoomToFeatures.zoomType == ZoomType.EXTENT) {
                if (bounds.getWidth() == 0.0 && bounds.getHeight() == 0.0) {
                    // single point, so we directly set the center
                    Coordinate center = bounds.centre();
                    mapValues.center = new double[] {center.x, center.y};
                    mapValues.bbox = null;
                    mapValues.scale = mapValues.zoomToFeatures.minScale;
                    mapValues.recalculateBounds();
                } else {
                    mapValues.bbox = new double[] {
                            bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY()};
                    mapValues.center = null;
                    mapValues.recalculateBounds();

                    MapBounds mapBounds = mapValues.getMapBounds();
                    Rectangle paintArea = new Rectangle(mapValues.getMapSize());
                    // expand the bounds so that they match the ratio of the paint area
                    mapBounds = mapBounds.adjustedEnvelope(paintArea);

                    if (mapValues.zoomToFeatures.minMargin != null) {
                        // add a margin around the feature bounds
                        mapBounds = ((BBoxMapBounds) mapBounds).expand(mapValues.zoomToFeatures.minMargin, paintArea);
                    }

                    final double scaleDenominator = mapBounds.getScaleDenominator(paintArea, mapValues.getDpi());
                    if (scaleDenominator < mapValues.zoomToFeatures.minScale) {
                        // if the current scale is smaller than the min. scale, change it
                        mapBounds = mapBounds.zoomToScale(mapValues.zoomToFeatures.minScale);
                    }

                    if (mapValues.isUseNearestScale()) {
                        // if fixed scales are used, take next higher scale
                        mapBounds = mapBounds.adjustBoundsToNearestScale(
                                mapValues.getZoomLevels(), 0.0,
                                ZoomLevelSnapStrategy.HIGHER_SCALE,
                                mapValues.getZoomSnapGeodetic(),
                                paintArea, mapValues.getRequestorDPI());
                    }

                    mapValues.setMapBounds(mapBounds);
                }
            }
        }
    }

    /**
     * Get the bounding-box containing all features of all layers.
     */
    private ReferencedEnvelope getFeatureBounds(
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final MapAttributeValues mapValues, final ExecutionContext context) {
        final MapfishMapContext mapContext = createMapContext(mapValues);

        String layerName = mapValues.zoomToFeatures.layer;
        ReferencedEnvelope bounds = null;
        for (MapLayer layer : mapValues.getLayers()) {
            checkCancelState(context);

            if ((!Strings.isNullOrEmpty(layerName) && layerName.equals(layer.getName())) ||
                    (Strings.isNullOrEmpty(layerName) && layer instanceof AbstractFeatureSourceLayer &&
                            !(layer instanceof GridLayer))) {
                AbstractFeatureSourceLayer featureLayer = (AbstractFeatureSourceLayer) layer;
                FeatureSource<?, ?> featureSource = featureLayer.getFeatureSource(clientHttpRequestFactory, mapContext);
                FeatureCollection<?, ?> features;
                try {
                    features = featureSource.getFeatures();
                } catch (IOException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }

                if (!features.isEmpty()) {
                    if (bounds == null) {
                        bounds = features.getBounds();
                    } else {
                        bounds.expandToInclude(features.getBounds());
                    }
                }
            }
        }

        return bounds;
    }

    /**
     * Set the type of buffered image rendered to.  See {@link org.mapfish.print.processor.map.CreateMapProcessor.BufferedImageType}.
     * <p></p>
     * Default is {@link org.mapfish.print.processor.map.CreateMapProcessor.BufferedImageType#TYPE_4BYTE_ABGR}.
     *
     * @param imageType one of the {@link org.mapfish.print.processor.map.CreateMapProcessor.BufferedImageType} values.
     */
    public void setImageType(final String imageType) {
        this.imageType = BufferedImageType.lookupValue(imageType);
    }

    /**
     * The Input object for processor.
     */
    public static class Input {
        /**
         * A factory for making http requests.  This is added to the values by the framework and therefore
         * does not need to be set in configuration
         */
        public MfClientHttpRequestFactory clientHttpRequestFactory;

        /**
         * The required parameters for the map.
         */
        public MapAttribute.MapAttributeValues map;

        /**
         * The path to the temporary directory for the print task.
         */
        public File tempTaskDirectory;
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

        private Output(final List<URI> layerGraphics,
                       final String mapSubReport,
                       final MapfishMapContext mapContext) {
            this.layerGraphics = layerGraphics;
            this.mapSubReport = mapSubReport;
            this.mapContext = mapContext;
        }
    }

    private static final class OpacityAdjustingStyleHandler extends DefaultStyleHandler {
        @Override
        public void setStyle(final Element element,
                             final Map styleMap,
                             final SVGGeneratorContext generatorContext) {
            String tagName = element.getTagName();
            Iterator iter = styleMap.keySet().iterator();
            while (iter.hasNext()) {
                String styleName = (String) iter.next();
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
}
