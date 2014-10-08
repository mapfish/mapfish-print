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

package org.mapfish.print.map.tiled;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import org.mapfish.print.parser.HasDefaultValue;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

/**
 * Contains the standard parameters for tiled layers.
 *
 * @author Jesse on 4/3/14.
 *         // CSOFF:VisibilityModifier
 */
public abstract class AbstractTiledLayerParams {
    /**
     * The opacity of the image.
     */
    @HasDefaultValue
    public double opacity = 1.0;
    /**
     * The name of the style (in Configuration or Template) to use when drawing the layer to the map.  This is separate from
     * the style in that it indicates how to draw the map.  It allows one to apply any of the SLD raster styling.
     */
    @HasDefaultValue
    public String rasterStyle = "raster";
    /**
     * The format of the image.  It is not a mimetype just the part after the image.  for example png, gif, tiff, tif, bmp, etc...
     * <p/>
     * If a protocol needs a mimetype it can add the prefix
     */
    @HasDefaultValue
    public String imageFormat = "png";

    /**
     * Get the base url for all tile requests.  For example it might be 'http://server.com/geoserver/gwc/service/wmts'.
     */
    public abstract String getBaseUrl();

    /**
     * Validates the provided base url.
     * @return True, if the url is valid.
     */
    public abstract boolean validateBaseUrl();

    /**
     * Create a URL that is common to all image requests for this layer.  It will take the base url and append all mergeable and
     * custom params to the base url.
     *
     * @param queryParamCustomization a function that can optionally modify the Multimap passed into the function and returns the
     *                                Multimap that will contain all the query params that will be part of the URI.  If the function
     *                                returns null then the original map will be used as the params.
     */
    public abstract String createCommonUrl(Function<Multimap<String, String>, Multimap<String, String>> queryParamCustomization)
            throws URISyntaxException, UnsupportedEncodingException;
}
