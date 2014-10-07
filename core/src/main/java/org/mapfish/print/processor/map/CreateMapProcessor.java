/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.processor.map;

import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Polygon;
import net.sf.jasperreports.engine.JRException;
import org.apache.batik.svggen.DefaultStyleHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.AreaOfInterest;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.map.geotools.FeatureLayer;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.processor.jasper.MapSubReport;
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
 * @author jesseeichar on 3/17/14.
 * @author sbrunner
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
        final List<URI> graphics = createLayerGraphics(param.tempTaskDirectory, param.clientHttpRequestFactory,
                mapValues, context);
        checkCancelState(context);
        final URI mapSubReport = createMapSubReport(param.tempTaskDirectory, mapValues.getMapSize(), graphics, mapValues.getDpi());

        return new Output(graphics, mapSubReport.toString(), createMapContext(mapValues));
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
        final MapSubReport subReport = new MapSubReport(graphics, mapSize, dpi);

        final File compiledReport = File.createTempFile("map-",
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, printDirectory);
        subReport.compile(compiledReport);

        return compiledReport.toURI();
    }

    private List<URI> createLayerGraphics(final File printDirectory,
                                          final MfClientHttpRequestFactory clientHttpRequestFactory,
                                          final MapAttribute.MapAttributeValues mapValues,
                                          final ExecutionContext context)
            throws Exception {
        final MapfishMapContext transformer = createMapContext(mapValues);

        // reverse layer list to draw from bottom to top.  normally position 0 is top-most layer.
        final List<MapLayer> layers = Lists.reverse(Lists.newArrayList(mapValues.getLayers()));

        final AreaOfInterest areaOfInterest = addAreaOfInterestLayer(mapValues, layers);

        final String mapKey = UUID.randomUUID().toString();
        final List<URI> graphics = new ArrayList<URI>(layers.size());
        int i = 0;
        for (MapLayer layer : layers) {
            checkCancelState(context);
            boolean isFirstLayer = i == 0;

            File path = null;
            if (renderAsSvg(layer)) {
                // render layer as SVG
                final SVGGraphics2D graphics2D = getSvgGraphics(transformer.getMapSize());

                try {
                    Graphics2D clippedGraphics2D = createClippedGraphics(transformer, areaOfInterest, graphics2D);
                    layer.render(clippedGraphics2D, clientHttpRequestFactory, transformer, isFirstLayer);

                    path = new File(printDirectory, mapKey + "_layer_" + i + ".svg");
                    saveSvgFile(graphics2D, path);
                } finally {
                    graphics2D.dispose();
                }
            } else {
                // render layer as raster graphic
                final BufferedImage bufferedImage = new BufferedImage(transformer.getMapSize().width,
                        transformer.getMapSize().height, this.imageType.value);
                Graphics2D graphics2D = createClippedGraphics(transformer, areaOfInterest, bufferedImage.createGraphics());
                try {
                    layer.render(graphics2D, clientHttpRequestFactory, transformer, isFirstLayer);

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

    private MapfishMapContext createMapContext(final MapAttribute.MapAttributeValues mapValues) {
        final Dimension mapSize = mapValues.getMapSize();
        Rectangle paintArea = new Rectangle(mapSize);

        final double dpi = mapValues.getDpi();
        final double dpiOfRequestor = mapValues.getRequestorDPI();

        MapBounds bounds = mapValues.getMapBounds();
        bounds = adjustBoundsToScaleAndMapSize(mapValues, dpiOfRequestor, paintArea, bounds);

        // if the DPI is higher than the PDF DPI we need to make the image larger so the image put in the PDF is large enough for the
        // higher DPI printer
        final double dpiRatio = dpi / dpiOfRequestor;
        paintArea.setBounds(0, 0, (int) (mapSize.getWidth() * dpiRatio), (int) (mapSize.getHeight() * dpiRatio));
        return new MapfishMapContext(bounds, paintArea.getSize(),
                mapValues.getRotation(), dpi, mapValues.getRequestorDPI(), mapValues.longitudeFirst, mapValues.isDpiSensitiveStyle());
    }


    private AreaOfInterest addAreaOfInterestLayer(@Nonnull final MapAttribute.MapAttributeValues mapValues,
                                                  @Nonnull final List<MapLayer> layers) throws IOException {
        final AreaOfInterest areaOfInterest = mapValues.areaOfInterest;
        if (areaOfInterest != null && areaOfInterest.display == AreaOfInterest.AoiDisplay.RENDER) {
            FeatureLayer.FeatureLayerParam param = new FeatureLayer.FeatureLayerParam();
            param.defaultStyle = Constants.Style.OverviewMap.NAME;
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
                                             @Nonnull final Graphics2D graphics2D) {
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
            final AffineTransform worldToScreenTransform = worldToScreenTransform(transformer.toReferencedEnvelope(),
                    transformer.getPaintArea());

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
                    mapValues.getZoomLevelSnapStrategy(), paintArea, dpiOfRequestor);
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

    /**
     * Set the type of buffered image rendered to.  See {@link org.mapfish.print.processor.map.CreateMapProcessor.BufferedImageType}.
     * <p/>
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
