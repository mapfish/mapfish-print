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

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Represent Map Bounds with a center location and a scale of the map.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class CenterScaleMapBounds implements MapBounds {
    private Coordinate center;
    private double scale;

    /**
     * Constructor.
     *
     * @param centerX the x coordinate of the center point.
     * @param centerY the y coordinate of the center point.
     * @param scale   the scale denominator of the map
     */
    public CenterScaleMapBounds(final double centerX, final double centerY, final double scale) {
        this.center = new Coordinate(centerX, centerY);
        this.scale = scale;
    }
}
