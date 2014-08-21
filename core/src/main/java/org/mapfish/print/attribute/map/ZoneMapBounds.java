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

import com.vividsolutions.jts.geom.Geometry;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author Stéphane Brunner
 */
public class ZoneMapBounds extends BBoxMapBounds {
    private final Geometry zone;

    /**
     * Constructor.
     *
     * @param projection the projection these bounds are defined in.
     * @param zone       the zone who we want to print.
     */
    public ZoneMapBounds(final CoordinateReferenceSystem projection, final Geometry zone) {
        super(projection, zone.getEnvelopeInternal());
        this.zone = zone;
    }

    // CSOFF: DesignForExtension

    @Override
    public Geometry getZone() {
        return this.zone;
    }

    // CSON: DesignForExtension

    // CHECKSTYLE:OFF

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ZoneMapBounds that = (ZoneMapBounds) o;

        return zone.equals(that.zone);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + zone.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ZoneMapBounds{geom=" + this.zone + '}';
    }
    // CHECKSTYLE:ON
}
