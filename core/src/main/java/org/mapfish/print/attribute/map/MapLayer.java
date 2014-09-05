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

package org.mapfish.print.attribute.map;

import com.google.common.base.Optional;
import org.mapfish.print.http.MfClientHttpRequestFactory;

import java.awt.Graphics2D;

/**
 * Encapsulates the data required to load map data for a layer and render it.
 *
 * @author Jesse on 3/26/14.
 */
public interface MapLayer {

    /**
     * Attempt to add the layer this layer so that both can be rendered as a single layer.
     * <p/>
     * For example:
     * 2 WMS layers from the same WMS server can be combined into a single WMS layer and the map can be rendered
     * with a single WMS request.
     *
     * @param newLayer the layer to combine with this layer.  The new layer will be rendered <em>below</em> the current layer.
     * @return If the two layers can be combined then a map layer representing the two layers will be returned.  If the two layers
     * cannot be combined then Option.absent() will be returned.
     */
    Optional<MapLayer> tryAddLayer(MapLayer newLayer);

    /**
     * Render the layer to the graphics2D object.
     * @param graphics2D   the graphics object.
     * @param clientHttpRequestFactory The factory to use for making http requests.
     * @param transformer  the map transformer containing the map bounds and size
     * @param isFirstLayer true indicates this layer is the first layer in the map (the first layer drawn, ie the base layer)
     */
    void render(Graphics2D graphics2D,
                MfClientHttpRequestFactory clientHttpRequestFactory,
                MapfishMapContext transformer,
                final boolean isFirstLayer);

    /**
     * Indicate if the layer supports native rotation (e.g. WMS layers with 
     * the "angle" parameter).
     * @return True if the layer itself takes care of rotating.
     */
    boolean supportsNativeRotation();
}
