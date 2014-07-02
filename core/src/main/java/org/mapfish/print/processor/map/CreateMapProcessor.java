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
import net.sf.jasperreports.engine.JRException;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapTransformer;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.processor.jasper.MapSubReport;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.w3c.dom.Document;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


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
        final URI mapSubReport = createMapSubReport(param.tempTaskDirectory,
                mapValues.getMapSize(), graphics);

        return new Output(graphics, mapSubReport.toString());
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.imageType == null) {
            validationErrors.add(new ConfigurationException("No imageType defined in " + getClass().getName()));
        }
    }

    private URI createMapSubReport(final File printDirectory, final Dimension mapSize,
            final List<URI> graphics) throws IOException, JRException {
        final MapSubReport subReport = new MapSubReport(graphics, mapSize);
        
        final File compiledReport = File.createTempFile("map-",
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, printDirectory);
        subReport.compile(compiledReport);
        
        return compiledReport.toURI();
    }

    private List<URI> createLayerGraphics(final File printDirectory,
                                          final ClientHttpRequestFactory clientHttpRequestFactory,
                                          final MapAttribute.MapAttributeValues mapValues,
                                          final ExecutionContext context)
            throws Exception {
        final Dimension mapSize = mapValues.getMapSize();
        final double dpi = mapValues.getDpi();
        Rectangle paintArea = new Rectangle(mapSize);

        // We are making the same assumption as Openlayers 2.x versions, that the DPI is 72.
        // In the future we probably need to change this assumption and allow the client software to
        // specify the DPI they are using for creating the bounds.
        // For the moment we require the client to convert their bounds to 72 DPI
        final double dpiOfRequestor = Constants.PDF_DPI;

        MapBounds bounds = mapValues.getMapBounds();

        if (mapValues.isUseNearestScale()) {
                bounds = bounds.adjustBoundsToNearestScale(
                        mapValues.getZoomLevels(),
                        mapValues.getZoomSnapTolerance(),
                        mapValues.getZoomLevelSnapStrategy(), paintArea, dpiOfRequestor);
        }
        
        bounds = new BBoxMapBounds(bounds.toReferencedEnvelope(paintArea, dpiOfRequestor));

        if (mapValues.isUseAdjustBounds()) {
            bounds = bounds.adjustedEnvelope(paintArea);
        }

        // if the DPI is higher than the PDF DPI we need to make the image larger so it will be
        final double dpiRatio = dpi / dpiOfRequestor;
        paintArea.setBounds(0, 0, (int) (mapSize.getWidth() * dpiRatio), (int) (mapSize.getHeight() * dpiRatio));
        final MapTransformer transformer = new MapTransformer(bounds, paintArea.getSize(), mapValues.getRotation(), dpi);
        
        // reverse layer list to draw from bottom to top.  normally position 0 is top-most layer.
        final List<MapLayer> layers = Lists.reverse(mapValues.getLayers());

        final String mapKey = UUID.randomUUID().toString();
        final List<URI> graphics = new ArrayList<URI>(layers.size());
        int i = 0;
        for (MapLayer layer : layers) {
            checkCancelState(context);
            boolean isFirstLayer = i == 0;
            
            File path = null;
            if (renderAsSvg(layer)) {
                // render layer as SVG
                final SVGGraphics2D graphics2D = getSvgGraphics(mapSize);

                try {
                    layer.render(graphics2D, clientHttpRequestFactory, transformer, isFirstLayer);
                    
                    path = new File(printDirectory, mapKey + "_layer_" + i + ".svg");
                    saveSvgFile(graphics2D, path);
                } finally {
                    graphics2D.dispose();
                }
            } else {
                // render layer as raster graphic
                final BufferedImage bufferedImage = new BufferedImage((int) paintArea.getWidth(),
                        (int) paintArea.getHeight(), this.imageType.value);
                final Graphics2D graphics2D = bufferedImage.createGraphics();
                
                try {
                    layer.render(graphics2D, clientHttpRequestFactory, transformer, isFirstLayer);
                    
                    path = new File(printDirectory, mapKey + "_layer_" + i + ".tiff");
                    ImageIO.write(bufferedImage, "tiff", path);
                } finally {
                    graphics2D.dispose();
                }
            }
            graphics.add(path.toURI());
            i++;
        }
        
        return graphics;
    }
    
    private boolean renderAsSvg(final MapLayer layer) {
        if (layer instanceof AbstractFeatureSourceLayer) {
            AbstractFeatureSourceLayer featureLayer = (AbstractFeatureSourceLayer) layer;
            return featureLayer.shouldRenderAsSvg();
        }
        return false;
     }

    private SVGGraphics2D getSvgGraphics(final Dimension mapSize)
            throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.getDOMImplementation().createDocument(null, "svg", null);
        
        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
        ctx.setComment("Generated by GeoTools2 with Batik SVG Generator");
        
        SVGGraphics2D g2d = new SVGGraphics2D(ctx, true);
        g2d.setSVGCanvasSize(mapSize);
        
        return g2d;
    }

    private void saveSvgFile(final SVGGraphics2D graphics2d, final File path) throws IOException {
        final FileOutputStream fs = new FileOutputStream(path);

        Writer osw = null;
        try {
            osw = new BufferedWriter(new OutputStreamWriter(fs, "UTF-8"));
            graphics2d.stream(osw);
        } finally {
            if (osw != null) {
                osw.close();
            }
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
    public static final class Input {
        /**
         * A factory for making http requests.  This is added to the values by the framework and therefore
         * does not need to be set in configuration
         */
        public ClientHttpRequestFactory clientHttpRequestFactory;

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
        public final List<URI> layerGraphics;
        
        /**
         * The path to the compiled sub-report for the map.
         */
        public final String mapSubReport;

        private Output(final List<URI> layerGraphics, final String mapSubReport) {
            this.layerGraphics = layerGraphics;
            this.mapSubReport = mapSubReport;
        }
    }

}
