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

package org.mapfish.print;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
                    throw ExceptionUtils.getRuntimeException(e);
                }
            }

            result.put(key, value);
        }
        return result;
    }

    /**
     * Add the given params to the query.
     *
     * @param url            The query
     * @param params         The params to add
     * @param overrideParams A set of parameter names that must be overridden and not added
     * @return The new query
     * @throws URISyntaxException 
     */
    public static String addParams(final String url, final Multimap<String, String> params, final Set<String> overrideParams)
            throws URISyntaxException {
        return addParams(new URI(url), params, overrideParams).toString();
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
            throw ExceptionUtils.getRuntimeException(e);
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
                result.append(URLEncoder.encode(key, Constants.DEFAULT_ENCODING));
                result.append("=");
                result.append(URLEncoder.encode(val, Constants.DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw ExceptionUtils.getRuntimeException(e);
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
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    /**
     * Read all the data from the provided URI and return the data as a string.
     *
     * @param requestFactory Request factory for making the request.
     * @param uri            the uri to load data from.
     * @return the data in string form.
     */
    public static String toString(final MfClientHttpRequestFactory requestFactory, final URI uri) throws IOException {
        Closer closer = Closer.create();
        try {
            ClientHttpResponse response = closer.register(requestFactory.createRequest(uri, HttpMethod.GET).execute());

            InputStream input = closer.register(response.getBody());
            InputStreamReader reader = closer.register(new InputStreamReader(input, Constants.DEFAULT_ENCODING));
            BufferedReader bufferedReader = closer.register(new BufferedReader(reader));

            return CharStreams.toString(bufferedReader);
        } finally {
            closer.close();
        }
    }

    /**
     * Read all the data from the provided URI and return the data as a string.
     *
     * @param requestFactory Request factory for making the request.
     * @param url            the uri to load data from.
     * @return the data in string form.
     */
    public static String toString(final MfClientHttpRequestFactory requestFactory, final URL url) throws IOException {
        try {
            return toString(requestFactory, url.toURI());
        } catch (URISyntaxException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    /**
     * Set the replace of the uri and return the new URI.
     *
     * @param initialUri the starting URI, the URI to update
     * @param path    the path to set on the baeURI
     */
    public static URI setPath(final URI initialUri, final String path) {
        String finalPath = path;
        if (!finalPath.startsWith("/")) {
            finalPath = '/' + path;
        }
        try {
            if (initialUri.getHost() == null && initialUri.getAuthority() != null) {
                return new URI(initialUri.getScheme(), initialUri.getAuthority(), finalPath, initialUri.getQuery(),
                        initialUri.getFragment());
            } else {
                return new URI(initialUri.getScheme(), initialUri.getUserInfo(), initialUri.getHost(), initialUri.getPort(),
                        finalPath, initialUri.getQuery(), initialUri.getFragment());
            }
        } catch (URISyntaxException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }
}
