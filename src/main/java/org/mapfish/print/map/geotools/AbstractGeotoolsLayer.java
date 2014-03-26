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
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * @author Jesse on 3/26/14.
 */
public abstract class AbstractGeotoolsLayer implements MapLayer {
    @Override
    public final Optional<MapLayer> tryAddLayer(final MapLayer newLayer) {
        return null;
    }

    @Override
    public final void render(final Graphics2D graphics2D, final MapBounds bounds, final Rectangle paintArea) {
        List<? extends Layer> layers = getLayers();

        MapContent content = new MapContent();
        content.addLayers(layers);

        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(content);

    }

    /**
     * Get the {@link org.geotools.data.DataStore} object that contains the data for this layer.
     */
    protected abstract List<? extends Layer> getLayers();
}
