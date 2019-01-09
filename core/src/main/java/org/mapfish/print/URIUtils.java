package org.mapfish.print;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
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
     * Parse the URI and get all the parameters in map form.  Query name -&gt; List of Query values.
     *
     * @param uri uri to analyze
     */
    public static Multimap<String, String> getParameters(final URI uri) {
        return getParameters(uri.getRawQuery());
    }

    /**
     * Parse the URI and get all the parameters in map form.  Query name -&gt; List of Query values.
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
                    value = URLDecoder.decode(pair.substring(pos + 1), "UTF-8");
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
     * @param url The query
     * @param params The params to add
     * @param overrideParams A set of parameter names that must be overridden and not added
     * @return The new query
     * @throws URISyntaxException
     */
    public static String addParams(
            final String url, final Multimap<String, String> params, final Set<String> overrideParams)
            throws URISyntaxException {
        return addParams(new URI(url), params, overrideParams).toString();
    }

    /**
     * Add the given params to the query.
     *
     * @param uri The query
     * @param params The params to add
     * @param overrideParams A set of parameter names that must be overridden and not added
     * @return The new query
     */
    public static URI addParams(
            final URI uri, final Multimap<String, String> params, final Set<String> overrideParams) {
        if (params == null || params.isEmpty()) {
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
                result.append(origTxt, 0, fragmentStart);
            }
        } else {
            result.append(origTxt, 0, queryStart);
        }

        Map<String, Collection<String>> origParams = getParameters(uri).asMap();
        boolean first = true;
        for (Map.Entry<String, Collection<String>> param: params.asMap().entrySet()) {
            final String key = param.getKey();
            Collection<String> origList = origParams.remove(key);
            if (origList != null && (overrideParams == null || !overrideParams.contains(key))) {
                first = addParams(result, first, key, origList);
            }
            Collection<String> list = param.getValue();
            first = addParams(result, first, key, list);
        }

        for (Map.Entry<String, Collection<String>> param: origParams.entrySet()) {
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

    private static boolean addParams(
            final StringBuilder result, final boolean isFirstParam, final String key,
            final Collection<String> list) {
        boolean first = isFirstParam;
        for (String val: list) {
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
     * @param key the key/param name
     * @param value the value to insert
     */
    public static void addParamOverride(
            final Multimap<String, String> params, final String key, final String value) {
        params.removeAll(key);
        params.put(key, value);
    }

    /**
     * Add a parameter to the query params (the params map) if there is not existing value for that key.
     *
     * @param params the query parameters
     * @param key the key/param name
     * @param value the value to insert
     */
    public static void setParamDefault(
            final Multimap<String, String> params, final String key, final String value) {
        if (!params.containsKey(key)) {
            params.put(key, value);
        }
    }

    /**
     * Construct a new uri by replacing query parameters in initialUri with the query parameters provided.
     *
     * @param initialUri the initial/template URI
     * @param queryParams the new query parameters.
     */
    public static URI setQueryParams(final URI initialUri, final Multimap<String, String> queryParams) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry: queryParams.entries()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(entry.getKey()).append("=").append(entry.getValue());
        }
        try {
            if (initialUri.getHost() == null && initialUri.getAuthority() != null) {
                return new URI(initialUri.getScheme(), initialUri.getAuthority(), initialUri.getPath(),
                               queryString.toString(),
                               initialUri.getFragment());
            } else {
                return new URI(initialUri.getScheme(), initialUri.getUserInfo(), initialUri.getHost(),
                               initialUri.getPort(),
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
     * @param uri the uri to load data from.
     * @return the data in string form.
     */
    public static String toString(final MfClientHttpRequestFactory requestFactory, final URI uri)
            throws IOException {
        try (ClientHttpResponse response = requestFactory.createRequest(uri, HttpMethod.GET).execute()) {
            return IOUtils.toString(response.getBody(), Constants.DEFAULT_ENCODING);
        }
    }

    /**
     * Read all the data from the provided URI and return the data as a string.
     *
     * @param requestFactory Request factory for making the request.
     * @param url the uri to load data from.
     * @return the data in string form.
     */
    public static String toString(final MfClientHttpRequestFactory requestFactory, final URL url)
            throws IOException {
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
     * @param path the path to set on the baeURI
     */
    public static URI setPath(final URI initialUri, final String path) {
        String finalPath = path;
        if (!finalPath.startsWith("/")) {
            finalPath = '/' + path;
        }
        try {
            if (initialUri.getHost() == null && initialUri.getAuthority() != null) {
                return new URI(initialUri.getScheme(), initialUri.getAuthority(), finalPath,
                               initialUri.getQuery(),
                               initialUri.getFragment());
            } else {
                return new URI(initialUri.getScheme(), initialUri.getUserInfo(), initialUri.getHost(),
                               initialUri.getPort(),
                               finalPath, initialUri.getQuery(), initialUri.getFragment());
            }
        } catch (URISyntaxException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }
}
