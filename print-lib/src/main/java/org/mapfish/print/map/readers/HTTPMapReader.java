/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.readers;

import org.apache.log4j.Logger;
import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.MatchAllSet;
import org.pvalsecc.misc.URIUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public abstract class HTTPMapReader extends MapReader {
    public static final Logger LOGGER = Logger.getLogger(HTTPMapReader.class);

    protected final RenderingContext context;
    protected final PJsonObject params;
    protected final URI baseUrl;
    public static final Set<String> OVERRIDE_ALL = new MatchAllSet<String>();

    protected HTTPMapReader(RenderingContext context, PJsonObject params) {
        super(params);
        this.context = context;
        this.params = params;
        try {
            baseUrl = new URI(params.getString("baseURL"));
        } catch (Exception e) {
            throw new InvalidJsonValueException(params, "baseURL", params.getString("baseURL"), e);
        }

        checkSecurity(params);
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
        Map<String, List<String>> queryParams = new HashMap<String, List<String>>();

        try {
            PJsonObject customParams = params.optJSONObject("customParams");
            if (customParams != null) {
                final Iterator<String> customParamsIt = customParams.keys();
                while (customParamsIt.hasNext()) {
                    String key = customParamsIt.next();
                    URIUtils.addParam(queryParams, key, customParams.getString(key));
                }
            }

            TileRenderer formater = TileRenderer.get(getFormat());

            addCommonQueryParams(queryParams, transformer, srs, first);
            final URI commonUri = URIUtils.addParams(baseUrl, queryParams, OVERRIDE_ALL);

            renderTiles(formater, transformer, commonUri, parallelMapTileLoader);
        } catch (Exception e) {
            context.addError(e);
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
