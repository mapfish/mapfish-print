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

import com.google.common.io.Closer;
import com.vividsolutions.jts.util.Assert;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.image.AbstractSingleImageLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import javax.imageio.ImageIO;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

/**
 * Wms layer.
 *
 * @author Jesse on 4/10/2014.
 */
public final class WmsLayer extends AbstractSingleImageLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WmsLayer.class);
    private final WmsLayerParam params;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param styleSupplier   the style to use when drawing the constructed grid coverage on the map.
     * @param params          the params from the request data.
     */
    protected WmsLayer(final ExecutorService executorService,
                       final StyleSupplier<GridCoverage2D> styleSupplier,
                       final WmsLayerParam params) {
        super(executorService, styleSupplier);
        this.params = params;
    }

    @Override
    protected BufferedImage loadImage(final MfClientHttpRequestFactory requestFactory,
                                      final MapfishMapContext transformer,
                                      final boolean isFirstLayer) throws Throwable {
        final WmsLayerParam wmsLayerParam = this.params;
        final URI commonUri = new URI(wmsLayerParam.getBaseUrl());

        final Rectangle paintArea = transformer.getPaintArea();
        ReferencedEnvelope envelope = transformer.getBounds().toReferencedEnvelope(paintArea, transformer.getDPI());
        URI uri = WmsUtilities.makeWmsGetLayerRequest(requestFactory, wmsLayerParam, commonUri, paintArea.getSize(), envelope);

        Closer closer = Closer.create();
        try {
            final ClientHttpResponse response = closer.register(requestFactory.createRequest(uri, HttpMethod.GET).execute());

            Assert.equals(HttpStatus.OK, response.getStatusCode(), "Http status code for " + uri + " was not OK.  It was: " + response
                    .getStatusCode() + ".  The response message was: '" + response.getStatusText() + "'");

            final BufferedImage image = ImageIO.read(response.getBody());
            if (image == null) {
                LOGGER.warn("The URI: " + uri + " is an image format that can be decoded");
                return createErrorImage(paintArea);
            }

            return image;
        } finally {
            closer.close();
        }
    }

    private BufferedImage createErrorImage(final Rectangle area) {
        final BufferedImage bufferedImage = new BufferedImage(area.width, area.height, TYPE_INT_ARGB_PRE);
        final Graphics2D graphics = bufferedImage.createGraphics();
        try {
            // CSOFF: MagicNumber
            graphics.setBackground(new Color(255, 255, 255, 125));
            // CSON: MagicNumber

            graphics.clearRect(0, 0, area.width, area.height);
            return bufferedImage;
        } finally {
            graphics.dispose();
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
