package org.mapfish.print.map.image.wms;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.mapfish.print.Constants;
import org.mapfish.print.URIUtils;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.image.wms.WmsLayerParam.ServerType;
import org.opengis.referencing.FactoryException;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A few methods to help make wms requests for all types of wms layers.
 *
 * @author Jesse on 7/23/2014.
 */
public final class WmsUtilities {
    private WmsUtilities() {
        // intentionally empty
    }

    /**
     * Make a WMS getLayer request and return the image read from the server.
     *  @param requestFactory the factory for making http requests
     * @param wmsLayerParam the wms request parameters
     * @param commonURI the uri to use for the requests (excepting parameters of course.)
     * @param imageSize the size of the image to request
     * @param dpi the dpi of the image to request
     * @param bounds the area and projection of the request on the world.
     */
    public static URI makeWmsGetLayerRequest(final MfClientHttpRequestFactory requestFactory,
                                             final WmsLayerParam wmsLayerParam,
                                             final URI commonURI,
                                             final Dimension imageSize,
                                             final double dpi,
                                             final ReferencedEnvelope bounds) throws FactoryException, URISyntaxException, IOException {
        final GetMapRequest getMapRequest = WmsVersion.lookup(wmsLayerParam.version).getGetMapRequest(commonURI.toURL());
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
        extraParams.putAll(wmsLayerParam.getMergeableParams());
        extraParams.putAll(wmsLayerParam.getCustomParams());

        if (wmsLayerParam.serverType != null && dpi != Constants.PDF_DPI) {
            addDpiParam(extraParams, (int) Math.round(dpi), wmsLayerParam.serverType);
        }
        return URIUtils.addParams(getMapUri, extraParams, Collections.<String>emptySet());

    }

    private static void addDpiParam(final Multimap<String, String> extraParams,
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
            if (!contains(extraParams, "FORMAT_OPTIONS")) {
                extraParams.put("FORMAT_OPTIONS", "dpi:" + Integer.toString(dpi));
            } else if (!isDpiSet(extraParams)) {
                setDpiValue(extraParams, dpi);
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
        for (String key : map.keys()) {
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
        String searchKey = "FORMAT_OPTIONS";
        for (String key : extraParams.keys()) {
            if (key.equalsIgnoreCase(searchKey)) {
                for (String value : extraParams.get(key)) {
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
        String searchKey = "FORMAT_OPTIONS";
        for (String key : extraParams.keys()) {
            if (key.equalsIgnoreCase(searchKey)) {
                Collection<String> values = extraParams.removeAll(key);
                List<String> newValues = new ArrayList<String>();
                for (String value : values) {
                    if (!Strings.isNullOrEmpty(value)) {
                        value += ";dpi:" + Integer.toString(dpi);
                        newValues.add(value);
                    }
                }
                extraParams.putAll(key, newValues);
            }
        }
    }
}
