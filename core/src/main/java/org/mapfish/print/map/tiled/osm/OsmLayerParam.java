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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.map.tiled.AbstractTiledLayerParams;
import org.mapfish.print.parser.HasDefaultValue;

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
    private static final int[] DEFAULT_TILE_SIZE = new int[] {256, 256};
    private static final double[] DEFAULT_MAX_EXTENT = new double[] {-20037508.34, -20037508.34, 20037508.34, 20037508.34};
    private static final Double[] DEFAULT_RESOLUTIONS = new Double[] {156543.03390625, 78271.516953125, 39135.7584765625,
        19567.87923828125, 9783.939619140625, 4891.9698095703125, 2445.9849047851562, 1222.9924523925781, 611.4962261962891,
        305.74811309814453, 152.87405654907226, 76.43702827453613, 38.218514137268066, 19.109257068634033, 9.554628534317017,
        4.777314267158508, 2.388657133579254, 1.194328566789627, 0.5971642833948135};

    /**
     * The URL used for the tile requests.
     * <p>Supported formats:</p>
     * <ul>
     *  <li>The base part of the URL, for example 'http://tile.openstreetmap.org'. This results in an URL
     *  like 'http://tile.openstreetmap.org/12/123/456.png'.</li>
     *  <li>An URL template with the placeholders '{x}', '{y}' or '{-y}', and '{z}'. For example:
     *  'http://tile.openstreetmap.org/{z}/{x}/{y}.png'. <br>
     *  The placeholder '{-y}' provides support for OSGeo TMS tiles.
     *  </li>
     * </ul>
     */
    public String baseURL;
    /**
     * The maximum extent of the osm layer. Must have 4 coordinates, minX, minY, maxX, maxY
     * <p>Default: [-20037508.34, -20037508.34, 20037508.34, 20037508.34]</p>
     */
    @HasDefaultValue
    public double[] maxExtent = DEFAULT_MAX_EXTENT;

    /**
     * The size of each tile.  Must have 2 values: width, height
     * <p>Default: [256, 256]</p>
     */
    @HasDefaultValue
    public int[] tileSize = DEFAULT_TILE_SIZE;

    /**
     * The allowed resolutions for this layer.
     */
    @HasDefaultValue
    public Double[] resolutions = DEFAULT_RESOLUTIONS;
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
    public Double dpi = null;

    /**
     * Validate the properties have the correct values.
     * @throws URISyntaxException 
     */
    public void postConstruct() throws URISyntaxException {
        Assert.equals(NUMBER_OF_EXTENT_COORDS, this.maxExtent.length, "maxExtent must have exactly 4 elements to the array.  Was: " +
                                                                      Arrays.toString(this
                .maxExtent));
        Assert.equals(2, this.tileSize.length, "tileSize must have exactly 2 elements to the array.  Was: " + Arrays.toString(this
                .tileSize));
        Assert.isTrue(this.resolutions.length > 0, "resolutions must have at least one value");

        Arrays.sort(this.resolutions, Ordering.<Double>natural().reverse());
        Assert.isTrue(validateBaseUrl(), "invalid baseURL");
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
    public String getBaseUrl() {
        return this.baseURL;
    }

    public Dimension getTileSize() {
        return new Dimension(this.tileSize[0], this.tileSize[1]);
    }

    @Override
    public String createCommonUrl(
            final Function<Multimap<String, String>, Multimap<String, String>> function) {
        return getBaseUrl();
    }

    @Override
    public boolean validateBaseUrl() {
        String url = getBaseUrl();
        if (Strings.isNullOrEmpty(url)) {
            return false;
        }

        // replace placeholders with dummy values, then check if an URI can be created
        url = url
                .replace("{z}", "0")
                .replace("{x}", "0")
                .replace("{y}", "0")
                .replace("{-y}", "0");

        try {
            return new URI(url) != null;
        } catch (URISyntaxException exc) {
            return false;
        }
    }
}
