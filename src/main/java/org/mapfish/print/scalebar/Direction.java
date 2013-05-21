/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.scalebar;

/**
 * Specify a direction for the labels and the bar.
 */
public enum Direction {
    UP(0, 0.0),
    DOWN(0, 180),
    LEFT(1, -90),
    RIGHT(1, 90);

    private final int orientation;
    private final double angle;

    Direction(int orientation, double angle) {
        this.orientation = orientation;
        this.angle = angle;
    }

    public boolean isSameOrientation(Direction other) {
        return other.orientation == orientation;
    }

    public double getAngle() {
        return angle;
    }
}
