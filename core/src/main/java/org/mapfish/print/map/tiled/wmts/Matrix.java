package org.mapfish.print.map.tiled.wmts;

import org.locationtech.jts.util.Assert;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Arrays;

import static org.mapfish.print.Constants.OGC_DPI;

/**
 * A class representing a matrix.
 */
public class Matrix {

    /**
     * The id of the matrix.
     */
    public String identifier;
    /**
     * A 2 dimensional array containing number of tiles in the matrix for the columns (0) and rows (1).
     */
    public long[] matrixSize;
    /**
     * The scale denominator of the matrix.
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
        Assert.equals(2, this.topLeftCorner.length,
                      "topLeftCorner must have exactly 2 elements to the array.  Was: " +
                              Arrays.toString(this.topLeftCorner));
        Assert.equals(2, this.matrixSize.length,
                      "matrixSize must have exactly 2 elements to the array.  Was: " +
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

    /**
     * Get the resolution.
     *
     * @param projection The map projection
     */
    public final double getResolution(final CoordinateReferenceSystem projection) {
        return new Scale(this.scaleDenominator, projection, OGC_DPI).getResolution();
    }
}
