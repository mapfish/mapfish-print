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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.mapfish.print.URIUtils;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import javax.annotation.Nullable;

/**
 * An abstract layers params class for WM* layers (e.g. WMS or WMTS).
 */
public abstract class AbstractWMXLayerParams extends AbstractTiledLayerParams {

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
    public PObject customParams;
    private final Multimap<String, String> additionalCustomParam = HashMultimap.create();

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
     * Read the {@link #customParams} into a Multimap.
     */
    public final Multimap<String, String> getCustomParams() {
        Multimap<String, String> result = convertToMultiMap(this.customParams);
        result.putAll(this.additionalCustomParam);
        return result;
    }

    /**
     * Read the {@link #mergeableParams} into a Multimap.
     */
    public final Multimap<String, String> getMergeableParams() {
        return convertToMultiMap(this.mergeableParams);
    }

    private Multimap<String, String> convertToMultiMap(final PObject objectParams) {
        Multimap<String, String> params = HashMultimap.create();
        if (objectParams != null) {
            Iterator<String> customParamsIter = objectParams.keys();
            while (customParamsIter.hasNext()) {
                String key = customParamsIter.next();
                if (objectParams.isArray(key)) {
                    final PArray array = objectParams.optArray(key);
                    for (int i = 0; i < array.size(); i++) {
                        params.put(key, array.getString(i));
                    }
                } else {
                    params.put(key, objectParams.optString(key, ""));
                }
            }
        }

        return params;
    }


    @Override
    public final String createCommonUrl(
            @Nullable final Function<Multimap<String, String>, Multimap<String, String>> queryParamCustomization)
            throws URISyntaxException, UnsupportedEncodingException {
        Multimap<String, String> queryParams = HashMultimap.create();

        queryParams.putAll(getCustomParams());
        queryParams.putAll(getMergeableParams());

        if (queryParamCustomization != null) {
            Multimap<String, String> result = queryParamCustomization.apply(queryParams);
            if (result != null) {
                queryParams = result;
            }
        }
        final URI baseUri = new URI(getBaseUrl());
        return URIUtils.addParams(getBaseUrl(), queryParams, URIUtils.getParameters(baseUri).keySet());
    }

    /**
     * Set a custom parameter.
     * @param name the parameter name
     * @param value the parameter value
     */
    public final void setCustomParam(final String name, final String value) {
        this.additionalCustomParam.put(name, value);
    }

    @Override
    public final boolean validateBaseUrl() {
        try {
            return new URI(getBaseUrl()) != null;
        } catch (URISyntaxException exc) {
            return false;
        }
    }

}
