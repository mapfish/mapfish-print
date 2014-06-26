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

package org.mapfish.print.map.tiled.wmts;

import com.vividsolutions.jts.util.Assert;

import java.util.Arrays;

/**
 * A class representing a matrix.
 * // CSOFF:VisibilityModifier
 */
public class Matrix {

    /**
     * The id of the matrix.
     */
    public String identifier;
    /**
     * A 2 dimensional array containing number of tiles in the matrix for the columns (0) and rows (1).
     */
    public int[] matrixSize;
    /**
     * The scale of the matrix.
     */
    public double scaleDenominator;
    /**
     * A 2 dimensional array representing the width, height of the tile.
     */
    public int[] tileSize;
    /**
     * A 2 dimensional array representing the top-left corner of the tile.
     */
    public double[] topLeftCorner;

    /**
     * Validate the properties have the correct values.
     */
    public final void postConstruct() {
        Assert.equals(2, this.tileSize.length, "tileSize must have exactly 2 elements to the array.  Was: " +
                                               Arrays.toString(this.tileSize));
        Assert.equals(2, this.topLeftCorner.length, "topLeftCorner must have exactly 2 elements to the array.  Was: " +
                                                    Arrays.toString(this.topLeftCorner));
        Assert.equals(2, this.matrixSize.length, "matrixSize must have exactly 2 elements to the array.  Was: " +
                                                 Arrays.toString(this.matrixSize));
    }

    /**
     * Get the width of a tile.
     */
    public final int getTileWidth() {
        return this.tileSize[0];
    }

    /**
     * Get the height of a tile.
     */
    public final int getTileHeight() {
        return this.tileSize[0];
    }
}
