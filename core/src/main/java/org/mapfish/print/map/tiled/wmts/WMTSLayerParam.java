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
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.Constants;
import org.mapfish.print.URIUtils;
import org.mapfish.print.map.tiled.AbstractWMXLayerParams;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.PObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The parameters for configuration a WMTS layer.
 */
public final class WMTSLayerParam extends AbstractWMXLayerParams {
    /**
     * The ‘ResourceURL’ available in the WMTS capabilities.
     * <p/>
     * Example (for <code>requestEncoding: "KVP"</code>):
     * <pre><code>
     * baseUrl: "http://domain.com/wmts"
     * </code></pre>
     * <p/>
     * Example (for <code>requestEncoding: "REST"</code>):
     * <pre><code>
     * baseUrl: "http://domain.com/wmts/roads/{TileMatrixSet}/{TileMatrix}/{TileCol}/{TileRow}.png"
     * </code></pre>
     * The following URL template variables are replaced:
     * <ul>
     *  <li>{Layer}</li>
     *  <li>{style}</li>
     *  <li>{TileMatrixSet}</li>
     *  <li>{TileMatrix}</li>
     *  <li>{TileRow}</li>
     *  <li>{TileCol}</li>
     *  <li>{[DIMENSION.IDENTIFIER]}</li>
     * </ul>
     */
    public String baseURL;
    /**
     * The layer name.
     */
    public String layer;
    /**
     * The WMTS protocol version to use.
     */
    @HasDefaultValue
    public String version = "1.0.0";
    /**
     * The way to make the requests. Either <code>KVP</code> or <code>REST</code> (default).
     */
    @HasDefaultValue
    public RequestEncoding requestEncoding = RequestEncoding.REST;
    /**
     * The style name (for styles on the WMTS server).
     */
    @HasDefaultValue
    public String style = "";
    /**
     * The "sample" dimensions or image color bands to retrieve.
     * <p/>
     * This can be null, if so then the default dimensions will be returned.
     * If specified they must be dimensions supported by the server.
     * <p/>
     * These are keys to the {@link #dimensionParams}.
     */
    @HasDefaultValue
    public String[] dimensions;

    /**
     * The dpi of the returned images.
     * <p/>
     * By default this is the OGC default DPI.
     */
    @HasDefaultValue
    public double dpi = Constants.OGC_DPI;
    /**
     * Dictionary of dimensions name (Must be uppercase) => value.
     */
    @HasDefaultValue
    public PObject dimensionParams;

    /**
     * The format of the image. for example image/png, image/jpeg, etc...
     */
    @HasDefaultValue
    public String imageFormat = "image/png";
    /**
     * Reference/Identifier to a tileMatrixSet and limits.
     */
    public String matrixSet;
    /**
     * Array of matrix ids.
     * <p/>
     * Example:
     * <pre><code>
     * [{
     *   "identifier": "0",
     *   "matrixSize": [1, 1],
     *   "scaleDenominator": 4000,
     *   "tileSize": [256, 256],
     *   "topLeftCorner": [420000, 350000]
     *   }, ...]
     * </code></pre>
     */
    public Matrix[] matrices;

    @Override
    public String getBaseUrl() {
        return this.baseURL;
    }

    /**
     * Validate some of the properties of this layer.
     */
    public void postConstruct() {
        Assert.isTrue(validateBaseUrl(), "invalid baseURL");
    }

    @Override
    public String createCommonUrl()
            throws URISyntaxException, UnsupportedEncodingException {
        if (RequestEncoding.REST == this.requestEncoding) {
            return getBaseUrl();
        } else {
            Multimap<String, String> queryParams = HashMultimap.create();

            queryParams.putAll(getCustomParams());
            queryParams.putAll(getMergeableParams());

            final URI baseUri = new URI(getBaseUrl());
            return URIUtils.addParams(getBaseUrl(), queryParams, URIUtils.getParameters(baseUri).keySet());
        }
    }

    @Override
    public boolean validateBaseUrl() {
        String url = getBaseUrl();
        if (Strings.isNullOrEmpty(url)) {
            return false;
        }

        if (RequestEncoding.REST == this.requestEncoding) {
            if (!containsVariables(url)) {
                return false;
            }
            try {
                return WMTSLayer.createRestURI(url, "matrix", 0, 0, this) != null;
            } catch (URISyntaxException exc) {
                return false;
            }
        } else {
            return super.validateBaseUrl();
        }
    }

    private boolean containsVariables(final String url) {
       return url.contains("{TileMatrix}") && url.contains("{TileRow}") && url.contains("{TileCol}");
    }
}
