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

package org.mapfish.print.map.tiled.osm;

import com.google.common.collect.Ordering;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.Constants;
import org.mapfish.print.json.parser.HasDefaultValue;
import org.mapfish.print.map.tiled.AbstractTiledLayerParams;

import java.awt.Dimension;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * The parameters for configuration an OSM layer.
 */
public final class OsmLayerParam extends AbstractTiledLayerParams {
    private static final int NUMBER_OF_EXTENT_COORDS = 4;
    private static final double DEFAULT_RESOLUTION_TOLERANCE = 1.9;
    /**
     * the ‘ResourceURL’ available in the WMTS capabilities.
     */
    public String baseURL;
    /**
     * The maximum extent of the osm layer. Must have 4 coordinates, minX, minY, maxX, maxY
     */
    public double[] maxExtent;

    /**
     * The size of each tile.  Must have 2 values: width, height
     */
    public int[] tileSize;

    /**
     * The allowed resolutions for this layer.
     */
    public Double[] resolutions;
    /**
     * The amount of difference between a resolution and a target resolution to consider the two equal.  The value is a
     * value from 0-1.
     */
    @HasDefaultValue
    public double resolutionTolerance = DEFAULT_RESOLUTION_TOLERANCE;
    /**
     * The DPI of the OSM tiles.
     */
    @HasDefaultValue
    public double dpi = Constants.PDF_DPI;

    /**
     * Validate the properties have the correct values.
     */
    public void postConstruct() {
        Assert.equals(NUMBER_OF_EXTENT_COORDS, this.maxExtent.length, "maxExtent must have exactly 4 elements to the array.  Was: " +
                                                                      Arrays.toString(this
                .maxExtent));
        Assert.equals(2, this.tileSize.length, "tileSize must have exactly 2 elements to the array.  Was: " + Arrays.toString(this
                .tileSize));
        Assert.isTrue(this.resolutions.length > 0, "resolutions must have at least one value");

        Arrays.sort(this.resolutions, Ordering.<Double>natural().reverse());
    }

    /**
     * Get the max extent as a envelop object.
     */
    public Envelope getMaxExtent() {
        final int minX = 0;
        final int maxX = 1;
        final int minY = 2;
        final int maxY = 3;
        return new Envelope(this.maxExtent[minX], this.maxExtent[minY], this.maxExtent[maxX], this.maxExtent[maxY]);
    }

    @Override
    public URI getBaseUri() throws URISyntaxException {
        return new URI(this.baseURL);
    }

    public Dimension getTileSize() {
        return new Dimension(this.tileSize[0], this.tileSize[1]);
    }
}
