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
import com.google.common.io.Closer;
import com.vividsolutions.jts.util.Assert;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.mapfish.print.URIUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.map.image.AbstractSingleImageLayer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import javax.imageio.ImageIO;

/**
 * Wms layer.
 *
 * @author Jesse on 4/10/2014.
 */
public final class WmsLayer extends AbstractSingleImageLayer {

    private final WmsLayerParam params;
    private final ClientHttpRequestFactory requestFactory;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param rasterStyle     the style to use when drawing the constructed grid coverage on the map.
     * @param params          the params from the request data.
     * @param requestFactory  a factory for making http requests.
     */
    protected WmsLayer(final ExecutorService executorService, final Style rasterStyle, final WmsLayerParam params,
                       final ClientHttpRequestFactory requestFactory) {
        super(executorService, rasterStyle);
        this.params = params;
        this.requestFactory = requestFactory;
    }

    @Override
    protected BufferedImage loadImage(final MapBounds bounds, final Rectangle imageSize, final double dpi,
                                      final boolean isFirstLayer) throws Throwable {
        final URI commonURI = this.params.getBaseUri();


        ReferencedEnvelope envelope = bounds.toReferencedEnvelope(imageSize, dpi);
        final GetMapRequest getMapRequest = WmsVersion.lookup(this.params.version).getGetMapRequest(commonURI.toURL());
        getMapRequest.setBBox(envelope);
        getMapRequest.setDimensions(imageSize.width, imageSize.height);
        getMapRequest.setFormat(this.params.imageFormat);
        getMapRequest.setSRS(CRS.lookupIdentifier(envelope.getCoordinateReferenceSystem(), false));

        for (int i = 0; i < this.params.layers.length; i++) {
            String layer = this.params.layers[i];
            String style = "";
            if (this.params.styles != null) {
                style = this.params.styles[i];
            }
            getMapRequest.addLayer(layer, style);
        }
        final URI getMapUri = getMapRequest.getFinalURL().toURI();

        Multimap<String, String> extraParams = HashMultimap.create();
        extraParams.putAll(this.params.getMergeableParams());
        extraParams.putAll(this.params.getCustomParams());
        final URI uri = URIUtils.addParams(getMapUri, extraParams, Collections.<String>emptySet());

        Closer closer = Closer.create();
        try {
            final ClientHttpResponse response = closer.register(this.requestFactory.createRequest(uri, HttpMethod.GET).execute());

            Assert.equals(HttpStatus.OK, response.getStatusCode(), "Http status code for " + uri + " was not OK.  It was: " + response
                    .getStatusCode() + ".  The response message was: '" + response.getStatusText() + "'");

            return ImageIO.read(response.getBody());
        } finally {
            closer.close();
        }
    }

    /**
     * Get the HTTP params.
     *
     * @return the HTTP params
     */
    public WmsLayerParam getParams() {
        return this.params;
    }
    
    /**
     * If supported by the WMS server, a parameter "angle" can be set
     * on "customParams" or "mergeableParams". In this case the rotation
     * will be done natively by the WMS.
     */
    @Override
    public boolean supportsNativeRotation() {
        return this.params.getCustomParams().containsKey("angle") ||
                this.params.getMergeableParams().containsKey("angle");
    }
}
