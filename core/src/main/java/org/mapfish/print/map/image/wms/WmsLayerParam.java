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

package org.mapfish.print.map.image.wms;

import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.map.tiled.AbstractWMXLayerParams;
import org.mapfish.print.parser.HasDefaultValue;

import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Layer parameters for WMS layer.
 *
 * @author Jesse on 4/10/2014.
 */
public class WmsLayerParam extends AbstractWMXLayerParams {
    /**
     * The base URL for the WMS.  Used for making WMS requests.
     */
    public String baseURL;
    /**
     * The wms layer to request in the GetMap request.  The order is important.  It is the order that they will appear in the
     * request.
     */
    public String[] layers;

    /**
     * The styles to apply to the layers.  If this is defined there should be the same number as the layers and the style are applied
     * to the layer in the {@link #layers} field.
     */
    @HasDefaultValue
    public String[] styles;

    /**
     * The WMS version to use when making requests.
     */
    @HasDefaultValue
    public String version = "1.1.1";

    /**
     *  If true transform the map angle to customParams.angle for GeoServer, and customParams.map_angle for MapServer.
     */
    @HasDefaultValue
    public boolean useNativeAngle = false;

    @Override
    public final String getBaseUrl() {
        return this.baseURL;
    }

    /**
     * Validate some of the properties of this layer.
     */
    // CSOFF: DesignForExtension
    public void postConstruct() throws URISyntaxException {
        // CSON: DesignForExtension
        WmsVersion.lookup(this.version);
        Assert.isTrue(validateBaseUrl(), "invalid baseURL");

        Assert.isTrue(this.layers.length > 0, "There must be at least one layer defined for a WMS request to make sense");

        // OpenLayers 2 compatibility.  It will post a single empty style no matter how many layers there are

        if (this.styles != null && this.styles.length != this.layers.length && this.styles.length == 1 && this.styles[0].trim().isEmpty()) {
            this.styles = null;
        } else {
            Assert.isTrue(this.styles == null || this.layers.length == this.styles.length,
                    "If styles are defined then there must be one for each layer.  Number of layers: " + this.layers.length + "\nStyles: "
                    + Arrays.toString(this.styles));
        }
        if (!this.imageFormat.startsWith("image/")) {
            this.imageFormat = "image/" + this.imageFormat;
        }
    }
}
