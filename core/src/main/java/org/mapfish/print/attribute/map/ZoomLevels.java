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

import com.google.common.collect.Ordering;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.ConfigurationObject;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Encapsulates a sorted set of scale denominators representing the allowed scales.  The scales are sorted from largest
 * to smallest and the index starts at 0, where 0 is the largest scale (most zoomed out)
 *
 * @author Jesse on 4/1/14.
 */
public final class ZoomLevels implements ConfigurationObject {
    private double[] scales;

    /**
     * Constructor.
     *
     * @param scales do not need to be sorted or unique.
     */
    public ZoomLevels(final double... scales) {
        TreeSet<Double> sortedSet = new TreeSet<Double>(Ordering.natural().reverse());
        for (int i = 0; i < scales.length; i++) {
            sortedSet.add(scales[i]);
        }
        this.scales = new double[sortedSet.size()];
        int i = 0;
        for (Double aDouble : sortedSet) {
            this.scales[i] = aDouble;
            i++;
        }
    }

    /**
     * default constructor for constructing by spring.
     */
    public ZoomLevels() {
        // intentionally empty
    }

    public void setScales(final double[] scales) {
        this.scales = scales;
    }

    /**
     * The number of zoom levels.
     */
    public int size() {
        return this.scales.length;
    }

    /**
     * Get the zoom level at the given index.
     * @param index the index of the zoom level to access.
     */
    public double get(final int index) {
        return this.scales[index];
    }

    @Override
    public String toString() {
        return Arrays.toString(this.scales);
    }

    // CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZoomLevels that = (ZoomLevels) o;

        if (!Arrays.equals(scales, that.scales)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(scales);
    }


    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (scales == null || scales.length == 0) {
            validationErrors.add(new ConfigurationException("There are no scales defined in " + getClass().getName()));
        }
    }

    /**
     * Return a copy of the zoom level scale denominators. Scales are sorted greatest to least.
     */
    public double[] getScales() {
        double[] dest = new double[this.scales.length];
        System.arraycopy(this.scales, 0, dest, 0, this.scales.length);
        return dest;
    }

    // CHECKSTYLE:ON
}
