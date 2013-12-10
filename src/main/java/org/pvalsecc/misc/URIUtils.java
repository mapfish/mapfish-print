/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.misc;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public abstract class URIUtils {
    public static Map<String, List<String>> getParameters(URI uri) throws URISyntaxException, UnsupportedEncodingException {
        return getParameters(uri.getRawQuery());
    }

    public static Map<String, List<String>> getParameters(String rawQuery) throws URISyntaxException, UnsupportedEncodingException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        if (rawQuery == null) {
            return result;
        }

        StringTokenizer tokens = new StringTokenizer(rawQuery, "&");
        while (tokens.hasMoreTokens()) {
            String pair = tokens.nextToken();
            int pos = pair.indexOf('=');
            if (pos == -1) {
                throw new URISyntaxException(rawQuery, "Cannot find '=' sign");
            }
            String key = URLDecoder.decode(pair.substring(0, pos), "UTF-8");
            String value = URLDecoder.decode(pair.substring(pos + 1, pair.length()), "UTF-8");
            addParam(result, key, value);
        }
        return result;
    }

    /**
     * Add the given params to the query
     *
     * @param uri             The query
     * @param params          The params to add
     * @param overridenParams A set of parameter names that must be overriden and not added
     * @return The new query
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     */
    public static URI addParams(URI uri, Map<String, List<String>> params, Set<String> overridenParams) throws URISyntaxException, UnsupportedEncodingException {
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

        Map<String, List<String>> origParams = getParameters(uri);
        boolean first = true;
        for (Map.Entry<String, List<String>> param : params.entrySet()) {
            final String key = param.getKey();
            List<String> origList = origParams.remove(key);
            if (origList != null && (overridenParams == null || !overridenParams.contains(key))) {
                first = addParams(result, first, key, origList);
            }
            List<String> list = param.getValue();
            first = addParams(result, first, key, list);
        }

        for (Map.Entry<String, List<String>> param : origParams.entrySet()) {
            final String key = param.getKey();
            List<String> list = param.getValue();
            first = addParams(result, first, key, list);
        }

        if (uri.getFragment() != null) {
            result.append('#').append(uri.getRawFragment());
        }

        return new URI(result.toString());
    }

    private static boolean addParams(StringBuilder result, boolean first, String key, List<String> list) throws UnsupportedEncodingException {
        for (int i = 0; i < list.size(); i++) {
            String val = list.get(i);
            if (first) {
                result.append('?');
                first = false;
            } else {
                result.append('&');
            }
            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(val, "UTF-8"));
        }
        return first;
    }

    public static void addParam(Map<String, List<String>> params, String key, String value) {
        List<String> list = params.get(key);
        if (list == null) {
            list = new ArrayList<String>(1);
            params.put(key, list);
        }
        list.add(value);
    }

    public static void addParamOverride(Map<String, List<String>> params, String key, String value) {
        ArrayList<String> list = new ArrayList<String>(1);
        params.put(key, list);
        list.add(value);
    }

    public static void setParamDefault(Map<String, List<String>> params, String key, String value) {
        List<String> list = params.get(key);
        if (list == null) {
            list = new ArrayList<String>(1);
            params.put(key, list);
            list.add(value);
        }
    }
}
