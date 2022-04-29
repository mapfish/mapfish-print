package org.mapfish.print.map.image.wms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.referencing.CRS;
import org.mapfish.print.URIUtils;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.image.wms.WmsLayerParam.ServerType;
import org.opengis.referencing.FactoryException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A few methods to help make wms requests for all types of wms layers.
 */
public final class WmsUtilities {
    private static final String FORMAT_OPTIONS = "FORMAT_OPTIONS";

    private WmsUtilities() {
        // intentionally empty
    }

    /**
     * Make a WMS getLayer request and return the image read from the server.
     *
     * @param wmsLayerParam the wms request parameters
     * @param commonURI the uri to use for the requests (excepting parameters of course.)
     * @param imageSize the size of the image to request
     * @param dpi the dpi of the image to request
     * @param angle the angle of the image to request
     * @param bounds the area and projection of the request on the world.
     */
    public static URI makeWmsGetLayerRequest(
            final WmsLayerParam wmsLayerParam,
            final URI commonURI,
            final Dimension imageSize,
            final double dpi,
            final double angle,
            final ReferencedEnvelope bounds) throws FactoryException, URISyntaxException, IOException {
        if (commonURI == null || commonURI.getAuthority() == null) {
            throw new RuntimeException("Invalid WMS URI: " + commonURI);
        }
        String[] authority = commonURI.getAuthority().split(":");
        URL url;
        if (authority.length == 2) {
            url = new URL(
                    commonURI.getScheme(),
                    authority[0],
                    Integer.parseInt(authority[1]),
                    commonURI.getPath()
            );
        } else {
            url = new URL(
                    commonURI.getScheme(),
                    authority[0],
                    commonURI.getPath()
            );
        }
        final GetMapRequest getMapRequest = WmsVersion.lookup(wmsLayerParam.version).
                getGetMapRequest(url);
        getMapRequest.setBBox(bounds);
        getMapRequest.setDimensions(imageSize.width, imageSize.height);
        getMapRequest.setFormat(wmsLayerParam.imageFormat);
        getMapRequest.setSRS(CRS.lookupIdentifier(bounds.getCoordinateReferenceSystem(), false));

        for (int i = wmsLayerParam.layers.length - 1; i > -1; i--) {
            String layer = wmsLayerParam.layers[i];
            String style = "";
            if (wmsLayerParam.styles != null) {
                style = wmsLayerParam.styles[i];
            }
            getMapRequest.addLayer(layer, style);
        }
        final URI getMapUri = getMapRequest.getFinalURL().toURI();

        Multimap<String, String> extraParams = HashMultimap.create();
        if (commonURI.getQuery() != null) {
            for (NameValuePair pair: URLEncodedUtils.parse(commonURI, Charset.forName("UTF-8"))) {
                extraParams.put(pair.getName(), pair.getValue());
            }
        }
        extraParams.putAll(wmsLayerParam.getMergeableParams());
        extraParams.putAll(wmsLayerParam.getCustomParams());

        if (wmsLayerParam.serverType != null) {
            addDpiParam(extraParams, (int) Math.round(dpi), wmsLayerParam.serverType);
            if (wmsLayerParam.useNativeAngle && angle != 0.0) {
                addAngleParam(extraParams, angle, wmsLayerParam.serverType);
            }
        }
        return URIUtils.addParams(getMapUri, extraParams, Collections.emptySet());

    }

    private static void addDpiParam(
            final Multimap<String, String> extraParams,
            final int dpi, final ServerType type) {
        switch (type) {
            case MAPSERVER:
                if (!contains(extraParams, "MAP_RESOLUTION")) {
                    extraParams.put("MAP_RESOLUTION", Integer.toString(dpi));
                }
                break;
            case QGISSERVER:
                if (!contains(extraParams, "DPI")) {
                    extraParams.put("DPI", Integer.toString(dpi));
                }
                break;
            case GEOSERVER:
                if (!contains(extraParams, FORMAT_OPTIONS)) {
                    extraParams.put(FORMAT_OPTIONS, "dpi:" + Integer.toString(dpi));
                } else if (!isDpiSet(extraParams)) {
                    setDpiValue(extraParams, dpi);
                }
                break;
            default:
                break;
        }
    }

    private static void addAngleParam(
            final Multimap<String, String> extraParams, final double angle, final ServerType type) {
        switch (type) {
            case MAPSERVER:
                if (!contains(extraParams, "MAP_ANGLE")) {
                    extraParams.put("MAP_ANGLE", Double.toString(Math.toDegrees(angle)));
                }
                break;
            case QGISSERVER:
                break;
            case GEOSERVER:
                if (!contains(extraParams, "ANGLE")) {
                    extraParams.put("ANGLE", Double.toString(Math.toDegrees(angle)));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Checks if a map contains a key ignoring upper/lower case.
     */
    private static boolean contains(final Multimap<String, ?> map, final String searchKey) {
        for (String key: map.keys()) {
            if (key.equalsIgnoreCase(searchKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the DPI value is already set for GeoServer.
     */
    private static boolean isDpiSet(final Multimap<String, String> extraParams) {
        for (String key: extraParams.keys()) {
            if (key.equalsIgnoreCase(FORMAT_OPTIONS)) {
                for (String value: extraParams.get(key)) {
                    if (value.toLowerCase().contains("dpi:")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Set the DPI value for GeoServer if there are already FORMAT_OPTIONS.
     */
    private static void setDpiValue(final Multimap<String, String> extraParams, final int dpi) {
        for (String key: extraParams.keys()) {
            if (key.equalsIgnoreCase(FORMAT_OPTIONS)) {
                Collection<String> values = extraParams.removeAll(key);
                List<String> newValues = new ArrayList<>();
                for (String value: values) {
                    if (!StringUtils.isEmpty(value)) {
                        value += ";dpi:" + Integer.toString(dpi);
                        newValues.add(value);
                    }
                }
                extraParams.putAll(key, newValues);
                return;
            }
        }
    }

    /**
     * Create a WMS request putting the params in the URL (GET) or in the body (POST).
     *
     * @param httpRequestFactory the request factory
     * @param uri the URI, including the parameters
     * @param method the HTTP method
     * @return The request
     * @throws IOException
     */
    public static ClientHttpRequest createWmsRequest(
            final MfClientHttpRequestFactory httpRequestFactory, final URI uri,
            final HttpMethod method) throws IOException {
        switch (method) {
            case GET:
                return httpRequestFactory.createRequest(uri, method);
            case POST:
                final String params = uri.getQuery();
                final URI paramlessUri;
                try {
                    paramlessUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                                           uri.getPort(), uri.getPath(), null, null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                final ClientHttpRequest request = httpRequestFactory.createRequest(paramlessUri, method);
                final byte[] encodedParams = params.getBytes(StandardCharsets.UTF_8);
                request.getHeaders().set("Content-Type", "application/x-www-form-urlencoded");
                request.getHeaders().set("Charset", "utf-8");
                request.getHeaders().set("Content-Length", Integer.toString(encodedParams.length));
                request.getBody().write(encodedParams);
                return request;
            default:
                throw new RuntimeException("Unsupported WMS request method: " + method);
        }
    }
}
