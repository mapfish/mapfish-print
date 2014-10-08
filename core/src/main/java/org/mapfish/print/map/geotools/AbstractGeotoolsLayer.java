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

package org.mapfish.print.map.geotools;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Jesse on 3/26/14.
 */
public abstract class AbstractGeotoolsLayer implements MapLayer {

    private final ExecutorService executorService;

    /**
     * Constructor.
     *  @param executorService the thread pool for doing the rendering.
     *
     */
    protected AbstractGeotoolsLayer(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public final Optional<MapLayer> tryAddLayer(final MapLayer newLayer) {
        return Optional.absent();
    }

    @Override
    public final void render(final Graphics2D graphics2D,
                             final MfClientHttpRequestFactory clientHttpRequestFactory,
                             final MapfishMapContext transformer,
                             final boolean isFirstLayer) {
        Rectangle paintArea = new Rectangle(transformer.getMapSize());
        MapBounds bounds = transformer.getBounds();

        MapfishMapContext layerTransformer = transformer;
        if (transformer.getRotation() != 0.0 && !this.supportsNativeRotation()) {
            // if a rotation is set and the rotation can not be handled natively
            // by the layer, we have to adjust the bounds and map size
            paintArea = new Rectangle(transformer.getRotatedMapSize());
            bounds = transformer.getRotatedBounds();
            graphics2D.setTransform(transformer.getTransform());
            Dimension mapSize = new Dimension(paintArea.width, paintArea.height);
            layerTransformer = new MapfishMapContext(bounds, mapSize, transformer.getRotation(), transformer.getDPI(),
                    transformer.getRequestorDPI(), transformer.isForceLongitudeFirst(), transformer.isDpiSensitiveStyle());
        }


        MapContent content = new MapContent();
        try {
            List<? extends Layer> layers = getLayers(clientHttpRequestFactory, layerTransformer, isFirstLayer);
            content.addLayers(layers);

            StreamingRenderer renderer = new StreamingRenderer();

            RenderingHints hints = new RenderingHints(Collections.<RenderingHints.Key, Object>emptyMap());
            hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
            hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON));
            hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY));
            hints.add(new RenderingHints(RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_ENABLE));
            hints.add(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON));
            hints.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC));
            hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY));
            hints.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE));
            hints.add(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

            graphics2D.addRenderingHints(hints);
            renderer.setJava2DHints(hints);
            Map<String, Object> renderHints = Maps.newHashMap();
            if (transformer.isForceLongitudeFirst() != null) {
                renderHints.put(StreamingRenderer.FORCE_EPSG_AXIS_ORDER_KEY, transformer.isForceLongitudeFirst());
            }
            renderer.setRendererHints(renderHints);

            renderer.setMapContent(content);
            renderer.setThreadPool(this.executorService);

            final ReferencedEnvelope mapArea = bounds.toReferencedEnvelope(paintArea, transformer.getDPI());
            renderer.paint(graphics2D, paintArea, mapArea);
        } catch (Exception e) {
            throw ExceptionUtils.getRuntimeException(e);
        } finally {
            content.dispose();
        }
    }

    /**
     * Get the {@link org.geotools.data.DataStore} object that contains the data for this layer.
     *  @param httpRequestFactory the factory for making http requests
     * @param transformer the map transformer
     * @param isFirstLayer true indicates this layer is the first layer in the map (the first layer drawn, ie the base layer)
     */
    protected abstract List<? extends Layer> getLayers(MfClientHttpRequestFactory httpRequestFactory,
                                                       MapfishMapContext transformer,
                                                       final boolean isFirstLayer) throws Exception;
    
    //CHECKSTYLE:OFF: DesignForExtension - Set a default value for all sub classes.
    @Override
    public boolean supportsNativeRotation() {
        return false;
    }
    //CHECKSTYLE:ON
}
