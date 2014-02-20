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

package org.mapfish.print.map.readers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.MatchAllSet;
import org.pvalsecc.misc.StringUtils;
import org.pvalsecc.misc.URIUtils;

public abstract class HTTPMapReader extends MapReader {
    public static final Logger LOGGER = Logger.getLogger(HTTPMapReader.class);

    protected final RenderingContext context;
    protected final PJsonObject params;
    protected final Map<String, List<String>> paramsToMerge = new HashMap<String, List<String>>();
    protected final Map<String, PJsonObject> mergeableParams;
    protected final URI baseUrl;
    public static final Set<String> OVERRIDE_ALL = new MatchAllSet<String>();

    protected HTTPMapReader(RenderingContext context, PJsonObject params) {
        super(params);
        this.context = context;
        this.params = params;
        try {
            // gets mergeable parameters for this server baseURL, so that requests to the same server
            // can be merged
            mergeableParams = context.getMergeableParams(params.getString("baseURL"));
            buildParamsToMerge();
        } catch (Exception e) {
            throw new InvalidJsonValueException(params, "customParams", params.getJSONObject("customParams"), e);
        }
        try {
            baseUrl = new URI(params.getString("baseURL"));
        } catch (Exception e) {
            throw new InvalidJsonValueException(params, "baseURL", params.getString("baseURL"), e);
        }

        checkSecurity(params);
    }

    private void buildParamsToMerge() throws JSONException {
        PJsonObject customParams = params.optJSONObject("customParams");
        // move customParams that can be merged from customParams
        // to paramsToMerge
        if (customParams != null) {
            final List<String> toBeSkipped = new ArrayList<String>();
            final Iterator<String> customParamsIt = customParams.keys();
            while (customParamsIt.hasNext()) {
                String key = customParamsIt.next();
                if (mergeableParams.containsKey(key.toUpperCase())) {

                    String value = getMergeableValue(customParams, toBeSkipped,
                            key);
                    paramsToMerge.put(key.toUpperCase(), new ArrayList<String>(
                            Arrays.asList(value)));
                }
            }
            for (String key : toBeSkipped) {
                customParams.getInternalObj().remove(key);
            }
        }
        // add missing mergeable params: we always need a value to merge, 
        // because the list needs to be ordered
        for (String mergeable : mergeableParams.keySet()) {
            if (!paramsToMerge.containsKey(mergeable)) {
                paramsToMerge.put(mergeable.toUpperCase(),
                        new ArrayList<String>(Arrays.asList("")));
            }
        }
    }

    protected String getMergeableValue(PJsonObject customParams,
            final List<String> toBeSkipped, String key) throws JSONException {
        toBeSkipped.add(key);
        return customParams.getString(key);
    }

    @Override
    public boolean testMerge(MapReader other) {
        if (canMerge(other)) {
            HTTPMapReader http = (HTTPMapReader) other;
            // add all the mergeableParams
            for(String mergeable : mergeableParams.keySet()) {
                paramsToMerge.get(mergeable).addAll(http.paramsToMerge.get(mergeable));
            }
            return true;
        } else {
            return false;
        }
    }
	
    private void checkSecurity(PJsonObject params) {
        try {
            if (!context.getConfig().validateUri(baseUrl)) {
                throw new InvalidJsonValueException(params, "baseURL", baseUrl);
            }
        } catch (Exception e) {
            throw new InvalidJsonValueException(params, "baseURL", baseUrl, e);
        }
    }

    public void render(Transformer transformer, ParallelMapTileLoader parallelMapTileLoader, String srs, boolean first) {

        try {
            final URI commonUri = createCommonURI(transformer, srs, first);

            TileRenderer formatter = TileRenderer.get(getFormat());
            renderTiles(formatter, transformer, commonUri, parallelMapTileLoader);
        } catch (Exception e) {
            context.addError(e);
        }
    }

    protected URI createCommonURI(Transformer transformer, String srs, boolean first) throws URISyntaxException, UnsupportedEncodingException {
        Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
        PJsonObject customParams = params.optJSONObject("customParams");
        if (customParams != null) {
            final Iterator<String> customParamsIt = customParams.keys();
            while (customParamsIt.hasNext()) {
                String key = customParamsIt.next();
                URIUtils.addParam(queryParams, key, customParams.getString(key));
            }
        }
        addMergeableQueryParams(queryParams);

        addCommonQueryParams(queryParams, transformer, srs, first);
        return URIUtils.addParams(baseUrl, queryParams, OVERRIDE_ALL);
    }

    private void addMergeableQueryParams(Map<String, List<String>> queryParams) {
        for (String key : mergeableParams.keySet()) {
            List<String> values = paramsToMerge.get(key);
            List<String> valuesWithDefaults = new ArrayList<String>();

            PJsonObject mergeableParam = mergeableParams.get(key);
            String separator = mergeableParam.optString("separator", ",");
            // we include the parameter if at least one
            // of the related values is defined
            boolean includeParam = false;
            for (String value : values) {
                if (value == null || value.isEmpty()) {
                    value = mergeableParam.optString("defaultValue", "");
                } else {
                    includeParam = true;
                }
                valuesWithDefaults.add(value);
            }
            if (includeParam) {
                URIUtils.addParam(queryParams, key,
                        StringUtils.join(valuesWithDefaults, separator));
            }
        }
    }

    protected abstract void renderTiles(TileRenderer formater, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException, URISyntaxException;

    protected abstract TileRenderer.Format getFormat();

    /**
     * Adds the query parameters common to every tile
     */
    protected abstract void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first);

    public boolean canMerge(MapReader other) {
        if (opacity != other.opacity) {
            return false;
        }

        if (other instanceof HTTPMapReader) {
            HTTPMapReader http = (HTTPMapReader) other;
            PJsonObject customParams = params.optJSONObject("customParams");
            PJsonObject customParamsOther = http.params.optJSONObject("customParams");
            return baseUrl.equals(http.baseUrl) &&
                    (customParams != null ? customParams.equals(customParamsOther) : customParamsOther == null);
        } else {
            return false;
        }
    }
}
