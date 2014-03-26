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

import com.vividsolutions.jts.geom.Envelope;

/**
 * Represent the map bounds with a bounding box.
 *
 * Created by Jesse on 3/26/14.
 */
public class BBoxMapBounds implements MapBounds {
    private final Envelope bbox;

    /**
     * Constructor.
     *
     * @param minx min X coordinate for the MapBounds
     * @param miny min Y coordinate for the MapBounds
     * @param maxx max X coordinate for the MapBounds
     * @param maxy max Y coordinate for the MapBounds
     */
    public BBoxMapBounds(final double minx, final double miny, final double maxx, final double maxy) {
        this.bbox = new Envelope(minx, maxx, miny, maxy);
    }
}
