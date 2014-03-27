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

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;

/**
 * Class Represents the bounds of the map in some way.  The implementations will represent the as a bbox or as a center and scale.
 * Created by Jesse on 3/26/14.
 */
public abstract class MapBounds {
    private final CoordinateReferenceSystem projection;

    /**
     * Constructor.
     *
     * @param projection the projection these bounds are defined in.
     */
    protected MapBounds(final CoordinateReferenceSystem projection) {
        this.projection = projection;
    }

    /**
     * Create a {@link org.geotools.geometry.jts.ReferencedEnvelope} representing the bounds.
     * <p/>
     *
     * @param paintArea the size of the map that will be drawn.
     * @param dpi
     * @return
     */
    public abstract ReferencedEnvelope toReferencedEnvelope(Rectangle paintArea, double dpi);
}
