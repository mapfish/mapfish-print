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

import org.mapfish.print.json.PJsonObject;

/**
 * Represent a scale denominator.  For example 1:10'000m which means 1 meter on the paper represent 10'000m on the ground.
 *
 * @author Jesse on 3/27/14.
 */
public final class Scale {
    private static final String SCALE = "scale";
    private static final String UNIT = "unit";
    private double denominator;
    private DistanceUnit unit;

    /**
     * Constructor.
     *
     * @param denominator the scale denominator.  a value of 1'000 would be a scale of 1:1'000
     * @param unit        the unit the scale is in.
     */
    public Scale(final double denominator, final DistanceUnit unit) {
        this.denominator = denominator;
        this.unit = unit;
    }

    /**
     * Construct by reading from request data.
     *
     * @param requestData the request data.
     */
    public Scale(final PJsonObject requestData) {
        this.denominator = requestData.getDouble(SCALE);
        this.unit = DistanceUnit.fromString(requestData.getString(UNIT));
    }

    public double getDenominator() {
        return this.denominator;
    }

    public DistanceUnit getUnit() {
        return this.unit;
    }
}
