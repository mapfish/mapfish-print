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
import org.mapfish.print.Constants;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Utility methods for editing and analyzing uris.
 */
public final class URIUtils {
    private URIUtils() {
        // intentionally empty
    }

    /**
     * Parse the URI and get all the parameters in map form.  Query name -> List of Query values.
     *
     * @param uri uri to analyze
     */
    public static Multimap<String, String> getParameters(final URI uri) {
        return getParameters(uri.getRawQuery());
    }

    /**
     * Parse the URI and get all the parameters in map form.  Query name -> List of Query values.
     *
     * @param rawQuery query portion of the uri to analyze.
     */
    public static Multimap<String, String> getParameters(final String rawQuery) {
        Multimap<String, String> result = HashMultimap.create();
        if (rawQuery == null) {
            return result;
        }

        StringTokenizer tokens = new StringTokenizer(rawQuery, "&");
        while (tokens.hasMoreTokens()) {
            String pair = tokens.nextToken();
            int pos = pair.indexOf('=');
            String key;
            String value;
            if (pos == -1) {
                key = pair;
                value = "";
            } else {

                try {
                    key = URLDecoder.decode(pair.substring(0, pos), "UTF-8");
                    value = URLDecoder.decode(pair.substring(pos + 1, pair.length()), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            result.put(key, value);
        }
        return result;
    }

    /**
     * Add the given params to the query.
     *
     * @param uri            The query
     * @param params         The params to add
     * @param overrideParams A set of parameter names that must be overridden and not added
     * @return The new query
     */
    public static URI addParams(final URI uri, final Multimap<String, String> params, final Set<String> overrideParams) {
        if (params == null || params.size() == 0) {
            return uri;
        }
        final String origTxt = uri.toString();
        int queryStart = origTxt.indexOf('?');
        final StringBuilder result = new StringBuilder();
        if (queryStart < 0) {
            int fragmentStart = origTxt.indexOf('#');
            if (fragmentStart < 0) {
                result.append(origTxt);
            } else {
                result.append(origTxt.substring(0, fragmentStart));
            }
        } else {
            result.append(origTxt.substring(0, queryStart));
        }

        Map<String, Collection<String>> origParams = getParameters(uri).asMap();
        boolean first = true;
        for (Map.Entry<String, Collection<String>> param : params.asMap().entrySet()) {
            final String key = param.getKey();
            Collection<String> origList = origParams.remove(key);
            if (origList != null && (overrideParams == null || !overrideParams.contains(key))) {
                first = addParams(result, first, key, origList);
            }
            Collection<String> list = param.getValue();
            first = addParams(result, first, key, list);
        }

        for (Map.Entry<String, Collection<String>> param : origParams.entrySet()) {
            final String key = param.getKey();
            Collection<String> list = param.getValue();
            first = addParams(result, first, key, list);
        }

        if (uri.getFragment() != null) {
            result.append('#').append(uri.getRawFragment());
        }

        try {
            return new URI(result.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean addParams(final StringBuilder result, final boolean isFirstParam, final String key,
                                     final Collection<String> list) {
        boolean first = isFirstParam;
        for (String val : list) {
            if (first) {
                result.append('?');
                first = false;
            } else {
                result.append('&');
            }
            try {
                result.append(URLEncoder.encode(key, Constants.ENCODING));
                result.append("=");
                result.append(URLEncoder.encode(val, Constants.ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return first;
    }

    /**
     * Add a parameter to the query params (the params map) replacing any parameter that might be there.
     *
     * @param params the query parameters
     * @param key    the key/param name
     * @param value  the value to insert
     */
    public static void addParamOverride(final Multimap<String, String> params, final String key, final String value) {
        params.removeAll(key);
        params.put(key, value);
    }

    /**
     * Add a parameter to the query params (the params map) if there is not existing value for that key.
     *
     * @param params the query parameters
     * @param key    the key/param name
     * @param value  the value to insert
     */
    public static void setParamDefault(final Multimap<String, String> params, final String key, final String value) {
        if (!params.containsKey(key)) {
            params.put(key, value);
        }
    }

    /**
     * Construct a new uri by replacing query parameters in initialUri with the query parameters provided.
     *
     * @param initialUri  the initial/template URI
     * @param queryParams the new query parameters.
     */
    public static URI setQueryParams(final URI initialUri, final Multimap<String, String> queryParams) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entries()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(entry.getKey()).append("=").append(entry.getValue());
        }
        try {
            if (initialUri.getHost() == null && initialUri.getAuthority() != null) {
                return new URI(initialUri.getScheme(), initialUri.getAuthority(), initialUri.getPath(), queryString.toString(),
                        initialUri.getFragment());
            } else {
                return new URI(initialUri.getScheme(), initialUri.getUserInfo(), initialUri.getHost(), initialUri.getPort(),
                        initialUri.getPath(),
                        queryString.toString(), initialUri.getFragment());
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
