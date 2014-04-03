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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.json.JSONArray;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.processor.HasDefaultValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

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
     * Custom query parameters to use when making http requests.  These are related to {@link #mergeableParams} except they
     * are the parameters that will prevent two layers from the same server from being merged into a single request with both
     * layers. See {@link #mergeableParams} for a more detailed example of the difference between {@link #mergeableParams} and
     * {@link #customParams}.
     * <p/>
     * The json should look something like:
     * <pre><code>
     * {
     *     "param1Name": "value",
     *     "param2Name": ["value1", "value2"]
     * }
     * </code></pre>
     */
    @HasDefaultValue
    public PJsonObject customParams;
    /**
     * Custom query parameters that can be merged if multiple layers are merged together into a single request.
     * <p/>
     * The json should look something like:
     * <pre><code>
     * {
     *     "param1Name": "value",
     *     "param2Name": ["value1", "value2"]
     * }
     * </code></pre>
     * <p/>
     * For example in WMS the style parameter can be merged.  If there are several wms layers that can be merged
     * except they have different style parameters they can be merged because the style parameter can be merged.
     * <p/>
     * Compare that to DPI parameter (for QGIS wms mapserver).  if two layers have different DPI then the layers
     * cannot be merged.  In this case the DPI should <em>NOT</em> be one of the {@link #mergeableParams} it should
     * be one of the {@link #customParams}.
     */
    @HasDefaultValue
    public PJsonObject mergeableParams;
    /**
     * The mime type of the format to request the image to be returned in.  For example: image/png.
     */
    @HasDefaultValue
    public String format = "image/png";

    /**
     * Get the base url for all tile requests.  For example it might be http://server.com/geoserver/gwc/service/wmts
     */
    public abstract URI getBaseUri() throws URISyntaxException;

    /**
     * Read the {@link #customParams} into a Multimap.
     */
    public final Multimap<String, String> getCustomParams() {
        return convertToMultiMap(this.customParams);
    }

    /**
     * Read the {@link #mergeableParams} into a Multimap.
     */
    public final Multimap<String, String> getMergeableParams() {
        return convertToMultiMap(this.mergeableParams);
    }

    private Multimap<String, String> convertToMultiMap(final PJsonObject jsonParams) {
        Multimap<String, String> params = HashMultimap.create();
        if (jsonParams != null) {
            Iterator<String> customParamsIter = jsonParams.keys();
            while (customParamsIter.hasNext()) {
                String key = customParamsIter.next();
                final Object opt = jsonParams.getInternalObj().opt(key);
                if (opt instanceof JSONArray) {
                    PJsonArray array = new PJsonArray(jsonParams, (JSONArray) opt, key);

                    for (int i = 0; i < array.size(); i++) {
                        params.put(key, array.getString(i));
                    }
                } else if (opt != null) {
                    params.put(key, opt.toString());
                } else {
                    params.put(key, "");
                }
            }
        }

        return params;
    }
}
