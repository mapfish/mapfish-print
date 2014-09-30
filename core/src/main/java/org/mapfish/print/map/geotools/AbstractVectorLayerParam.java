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

import org.mapfish.print.parser.HasDefaultValue;

/**
 * Common parameters for geotools vector layers.
 *
 * @author Jesse on 7/2/2014.
 */
public abstract class AbstractVectorLayerParam {
    /**
     * The style name of a style to apply to the features during rendering.  The style name must map to a style in the
     * template or the configuration objects.
     * <p/>
     * If no style is defined then the default style for the geometry type will be used.
     */
    @HasDefaultValue
    public String style;
    /**
     * Indicates if the layer is rendered as SVG.
     * <p/>
     * (will default to {@link org.mapfish.print.config.Configuration#defaultStyle}).
     */
    @HasDefaultValue
    public Boolean renderAsSvg;
}
