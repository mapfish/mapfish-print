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

package org.mapfish.print.map;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.annotation.Nonnull;

/**
 * Represent a scale denominator.  For example 1:10'000m which means 1 meter on the paper represent 10'000m on the ground.
 *
 * @author Jesse on 3/27/14.
 */
public final class Scale {
    private final double denominator;

    /**
     * Constructor.
     *
     * @param denominator the scale denominator.  a value of 1'000 would be a scale of 1:1'000
     */
    public Scale(final double denominator) {
        this.denominator = denominator;
    }

    public double getDenominator() {
        return this.denominator;
    }

    /**
     * Calculate the resolution for this scale.
     *
     * @param projection the projection to perform the calculation in
     * @param dpi the dpi of the display device.
     */
    public double toResolution(@Nonnull final CoordinateReferenceSystem projection, final double dpi) {
        double normScale = normalizeScale(this.denominator);
        final double distancePerInch = DistanceUnit.fromProjection(projection).convertTo(normScale, DistanceUnit.IN);
        return 1.0 / (distancePerInch * dpi);
    }

    private double normalizeScale(final double scale) {
        if (scale > 1.0) {
            return (1.0 / scale);
        } else {
            return scale;
        }
    }

    /**
     * Construct a scale object from a resolution.
     *
     * @param resolution the resolution of the map
     * @param projection the projection of the map
     * @param dpi the dpi of the display device.
     */
    public static Scale fromResolution(final double resolution, @Nonnull final CoordinateReferenceSystem projection, final double dpi) {
        final double resolutionInInches = DistanceUnit.fromProjection(projection).convertTo(resolution, DistanceUnit.IN);
        return new Scale(resolutionInInches * dpi);
    }

    // CHECKSTYLE:OFF

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Scale scale = (Scale) o;

        if (Double.compare(scale.denominator, denominator) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(denominator);
        result = (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Scale{" + denominator + '}';
    }
    // CHECKSTYLE:ON

}
