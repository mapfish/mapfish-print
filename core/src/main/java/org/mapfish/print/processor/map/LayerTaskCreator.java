/*
 * Copyright (C) 2016  Camptocamp
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

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.batik.svggen.SVGGraphics2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapfish.print.attribute.map.AreaOfInterest;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.processor.Processor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import static org.geotools.renderer.lite.RendererUtilities.worldToScreenTransform;
import static org.mapfish.print.processor.map.CreateMapProcessor.getSvgGraphics;
import static org.mapfish.print.processor.map.CreateMapProcessor.saveSvgFile;

/**
 *
 * @author jenselme
 */
public class LayerTaskCreator {

    private final File printDirectory;
    private final MfClientHttpRequestFactory clientHttpRequestFactory;
    private final MapfishMapContext mapContext;
    private final AreaOfInterest areaOfInterest;
    private final String mapKey;

    /**
     * Create LayerTask from common parameters.
     *
     * @param printDirectory Directory in which the temporary images will be
     * stored.
     * @param clientHttpRequestFactory A factory for making http requests.
     * @param mapContext The map context for a print task
     * @param areaOfInterest A GeoJSON geometry that is essentially the area of
     * the area to draw on the map.
     * @param mapKey UUID for the map.
     */
    public LayerTaskCreator(final File printDirectory,
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final MapfishMapContext mapContext,
            final AreaOfInterest areaOfInterest,
            final String mapKey) {
        this.printDirectory = printDirectory;
        this.clientHttpRequestFactory = clientHttpRequestFactory;
        this.mapContext = mapContext;
        this.areaOfInterest = areaOfInterest;
        this.mapKey = mapKey;
    }

    /**
     * Create a LayerTask from the common a specific parameters.
     *
     * @param layer The layer to print.
     * @param context The execution context for a print task.
     * @param imageTypeValue The type of the image.
     * @param index The index of the layer.
     * @return The LayerTask to be executed.
     */
    public final LayerTask create(final MapLayer layer,
            final Processor.ExecutionContext context,
            final int imageTypeValue,
            final int index) {
        LayerTask task
                = new LayerTask(this.printDirectory, this.clientHttpRequestFactory, this.mapContext, this.areaOfInterest, this.mapKey);
        task.setup(layer, context, imageTypeValue, index);

        return task;
    }

    /**
     * The LayerTask to execute in parallel.
     */
    public final class LayerTask implements Callable<URI> {

        private final MapfishMapContext mapContext;
        private final AreaOfInterest areaOfInterest;
        private final MfClientHttpRequestFactory clientHttpRequestFactory;
        private final File printDirectory;
        private final String mapKey;
        private int imageType;
        private Processor.ExecutionContext context;
        private MapLayer layer;
        private int index;

        private LayerTask(final File printDirectory,
                final MfClientHttpRequestFactory clientHttpRequestFactory,
                final MapfishMapContext mapContext,
                final AreaOfInterest areaOfInterest,
                final String mapKey) {
            this.mapContext = mapContext;
            this.areaOfInterest = areaOfInterest;
            this.clientHttpRequestFactory = clientHttpRequestFactory;
            this.printDirectory = printDirectory;
            this.mapKey = mapKey;
        }

        /**
         * Apply the specific parameters to a task.
         *
         * @param mapLayer The layer to print.
         * @param executionContext The execution context for a print task.
         * @param imageTypeValue The type of the image.
         * @param layerIndex The index of the layer.
         */
        public void setup(final MapLayer mapLayer,
                final Processor.ExecutionContext executionContext,
                final int imageTypeValue,
                final int layerIndex) {
            this.layer = mapLayer;
            this.context = executionContext;
            this.imageType = imageTypeValue;
            this.index = layerIndex;
        }

        @Override
        public URI call() throws Exception {
            boolean isFirstLayer = this.index == 0;

            File path = null;
            if (renderAsSvg(this.layer)) {
                // render layer as SVG
                final SVGGraphics2D graphics2D = getSvgGraphics(this.mapContext.getMapSize());

                try {
                    Graphics2D clippedGraphics2D = createClippedGraphics(this.mapContext, this.areaOfInterest, graphics2D);
                    this.layer.render(clippedGraphics2D, this.clientHttpRequestFactory, this.mapContext, isFirstLayer);

                    path = new File(this.printDirectory, this.mapKey + "_layer_" + this.index + ".svg");
                    saveSvgFile(graphics2D, path);
                } finally {
                    graphics2D.dispose();
                }
            } else {
                // render layer as raster graphic
                final BufferedImage bufferedImage = new BufferedImage(this.mapContext.getMapSize().width,
                        this.mapContext.getMapSize().height, this.imageType);
                Graphics2D graphics2D = createClippedGraphics(this.mapContext, this.areaOfInterest, bufferedImage.createGraphics());
                try {
                    this.layer.render(graphics2D, this.clientHttpRequestFactory, this.mapContext, isFirstLayer);

                    path = new File(this.printDirectory, this.mapKey + "_layer_" + this.index + ".png");
                    ImageIO.write(bufferedImage, "png", path);
                } finally {
                    graphics2D.dispose();
                }
            }

            return path.toURI();
        }

        private boolean renderAsSvg(final MapLayer layerBis) {
            if (layerBis instanceof AbstractFeatureSourceLayer) {
                AbstractFeatureSourceLayer featureLayer = (AbstractFeatureSourceLayer) layerBis;
                return featureLayer.shouldRenderAsSvg();
            }
            return false;
        }

        private Graphics2D createClippedGraphics(@Nonnull final MapfishMapContext transformer,
                @Nullable final AreaOfInterest areaOfInterestBis,
                @Nonnull final Graphics2D graphics2D) {
            if (areaOfInterestBis != null && areaOfInterestBis.display == AreaOfInterest.AoiDisplay.CLIP) {
                final Polygon screenGeometry = areaOfInterestInScreenCRS(transformer, areaOfInterestBis);
                final ShapeWriter shapeWriter = new ShapeWriter();
                final Shape clipShape = shapeWriter.toShape(screenGeometry);
                return new ConstantClipGraphics2D(graphics2D, clipShape);
            }

            return graphics2D;
        }

        private Polygon areaOfInterestInScreenCRS(@Nonnull final MapfishMapContext transformer,
                @Nullable final AreaOfInterest areaOfInterestBis) {
            if (areaOfInterestBis != null) {
                final AffineTransform worldToScreenTransform = worldToScreenTransform(transformer.toReferencedEnvelope(),
                        transformer.getPaintArea());

                MathTransform mathTransform = new AffineTransform2D(worldToScreenTransform);
                final Polygon screenGeometry;
                try {
                    screenGeometry = (Polygon) JTS.transform(areaOfInterestBis.getArea(), mathTransform);
                } catch (TransformException e) {
                    throw new RuntimeException(e);
                }
                return screenGeometry;
            }

            return null;
        }
    }
}
