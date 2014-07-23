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

package org.mapfish.print.map.tiled.wms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import jsr166y.ForkJoinPool;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.mapfish.print.URIUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.map.Scale;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.image.wms.WmsVersion;
import org.mapfish.print.map.tiled.AbstractTiledLayer;
import org.mapfish.print.map.tiled.TileCacheInformation;
import org.opengis.referencing.FactoryException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.annotation.Nonnull;

/**
 * Strategy object for rendering WMS based layers.
 *
 * @author Stéphane Brunner on 22/07/2014.
 */
public final class TiledWmsLayer extends AbstractTiledLayer {
    private final TiledWmsLayerParam param;

    /**
     * Constructor.
     *
     * @param forkJoinPool  the thread pool for doing the rendering.
     * @param styleSupplier strategy for loading the style for this layer
     * @param param         the information needed to create WMS requests.
     */
    public TiledWmsLayer(
            final ForkJoinPool forkJoinPool,
            final StyleSupplier<GridCoverage2D> styleSupplier,
            final TiledWmsLayerParam param) {
        super(forkJoinPool, styleSupplier);
        this.param = param;
    }

    /**
     * Get the HTTP params.
     *
     * @return the HTTP params
     */
    public TiledWmsLayerParam getParams() {
        return this.param;
    }

    @Override
    protected TileCacheInformation createTileInformation(
            final MapBounds bounds, final Rectangle paintArea, final double dpi,
            final boolean isFirstLayer) {
        return new WmsTileCacheInformation(bounds, paintArea, dpi, isFirstLayer);
    }

    private final class WmsTileCacheInformation extends TileCacheInformation {

        public WmsTileCacheInformation(
                final MapBounds bounds, final Rectangle paintArea, final double dpi,
                final boolean isFirstLayer) {
            super(bounds, paintArea, dpi, TiledWmsLayer.this.param);
        }

        @Nonnull
        @Override
        public ClientHttpRequest getTileRequest(
                final ClientHttpRequestFactory httpRequestFactory,
                final URI commonUri,
                final ReferencedEnvelope tileBounds,
                final Dimension tileSizeOnScreen,
                final int column,
                final int row)
                throws IOException, URISyntaxException, FactoryException {

            final GetMapRequest getMapRequest = WmsVersion.lookup(TiledWmsLayer.this.param.version).getGetMapRequest(commonUri.toURL());
            getMapRequest.setBBox(tileBounds);
            getMapRequest.setDimensions(tileSizeOnScreen.width, tileSizeOnScreen.height);
            getMapRequest.setFormat(TiledWmsLayer.this.param.imageFormat);
            getMapRequest.setSRS(CRS.lookupIdentifier(tileBounds.getCoordinateReferenceSystem(), false));

            for (int i = 0; i < TiledWmsLayer.this.param.layers.length; i++) {
                String layer = TiledWmsLayer.this.param.layers[i];
                String style = "";
                if (TiledWmsLayer.this.param.styles != null) {
                    style = TiledWmsLayer.this.param.styles[i];
                }
                getMapRequest.addLayer(layer, style);
            }
            final URI getMapUri = getMapRequest.getFinalURL().toURI();

            Multimap<String, String> extraParams = HashMultimap.create();
            extraParams.putAll(TiledWmsLayer.this.param.getMergeableParams());

            URI uri = URIUtils.addParams(getMapUri, extraParams, Collections.<String>emptySet());
            return httpRequestFactory.createRequest(uri, HttpMethod.GET);
        }

        @Override
        protected void customizeQueryParams(final Multimap<String, String> result) {
            //not much query params for this protocol...
        }

        @Override
        public Scale getScale() {
            return WmsTileCacheInformation.this.bounds.getScaleDenominator(
                    WmsTileCacheInformation.this.paintArea, dpi);
        }

        @Override
        public double getLayerDpi() {
            return TiledWmsLayer.this.param.dpi;
        }

        @Override
        public Dimension getTileSize() {
            return TiledWmsLayer.this.param.getTileSize();
        }

        @Nonnull
        @Override
        protected ReferencedEnvelope getTileCacheBounds() {
            return new ReferencedEnvelope(
                    this.bounds.toReferencedEnvelope(paintArea, dpi),
                    this.bounds.getProjection());
        }
    }
}
