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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Jesse on 3/26/14.
 */
public abstract class AbstractGeotoolsLayer implements MapLayer {

    private final ExecutorService executorService;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     */
    protected AbstractGeotoolsLayer(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public final Optional<MapLayer> tryAddLayer(final MapLayer newLayer) {
        return null;
    }

    @Override
    public final void render(final Graphics2D graphics2D, final MapBounds bounds, final Rectangle paintArea, final double dpi,
                             final boolean isFirstLayer) {
        List<? extends Layer> layers = getLayers(bounds, paintArea, dpi, isFirstLayer);

        MapContent content = new MapContent();
        try {
            content.addLayers(layers);

            StreamingRenderer renderer = new StreamingRenderer();


            RenderingHints hints = new RenderingHints(Collections.<RenderingHints.Key, Object>emptyMap());
            hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED));
            hints.add(new RenderingHints(RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_DISABLE));
            hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED));
            hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_SPEED));
            hints.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR));
            hints.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE));
            hints.add(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_OFF));

            hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

            graphics2D.addRenderingHints(hints);
            renderer.setJava2DHints(hints);

            renderer.setMapContent(content);
            renderer.setThreadPool(this.executorService);
            final ReferencedEnvelope mapArea = bounds.toReferencedEnvelope(paintArea, dpi);
            renderer.paint(graphics2D, paintArea, mapArea);
        } finally {
            content.dispose();
        }

    }

    /**
     * Get the {@link org.geotools.data.DataStore} object that contains the data for this layer.
     *
     * @param bounds the map bounds
     * @param paintArea the area to paint
     * @param dpi the DPI to render at
     * @param isFirstLayer true indicates this layer is the first layer in the map (the first layer drawn, ie the base layer)
     */
    protected abstract List<? extends Layer> getLayers(MapBounds bounds, Rectangle paintArea, double dpi,
                                                       final boolean isFirstLayer);

}
