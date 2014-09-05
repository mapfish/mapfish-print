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

package org.mapfish.print.map.image.wms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.mapfish.print.URIUtils;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.opengis.referencing.FactoryException;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

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
     * @param bounds the area and projection of the request on the world.
     */
    public static URI makeWmsGetLayerRequest(final MfClientHttpRequestFactory requestFactory,
                                             final WmsLayerParam wmsLayerParam,
                                             final URI commonURI,
                                             final Dimension imageSize,
                                             final ReferencedEnvelope bounds) throws FactoryException, URISyntaxException, IOException {
        final GetMapRequest getMapRequest = WmsVersion.lookup(wmsLayerParam.version).getGetMapRequest(commonURI.toURL());
        getMapRequest.setBBox(bounds);
        getMapRequest.setDimensions(imageSize.width, imageSize.height);
        getMapRequest.setFormat(wmsLayerParam.imageFormat);
        getMapRequest.setSRS(CRS.lookupIdentifier(bounds.getCoordinateReferenceSystem(), false));

        for (int i = 0; i < wmsLayerParam.layers.length; i++) {
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
        return URIUtils.addParams(getMapUri, extraParams, Collections.<String>emptySet());

    }
}
