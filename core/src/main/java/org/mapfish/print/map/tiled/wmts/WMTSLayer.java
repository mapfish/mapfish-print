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

package org.mapfish.print.map.tiled.wmts;

import com.google.common.collect.Multimap;
import jsr166y.ForkJoinPool;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.map.tiled.AbstractTiledLayer;
import org.mapfish.print.map.tiled.TileCacheInformation;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class for loading data from a WMTS.
 *
 * @author Jesse on 4/3/14.
 */
public class WMTSLayer extends AbstractTiledLayer {
    private final WMTSLayerParam param;
    private final ClientHttpRequestFactory requestFactory;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param rasterStyle     the style to use when drawing the constructed grid coverage on the map.
     * @param param           the information needed to create WMTS requests.
     * @param requestFactory  A factory for making http request objects.
     */
    protected WMTSLayer(final ForkJoinPool executorService, final Style rasterStyle, final WMTSLayerParam param,
                        final ClientHttpRequestFactory requestFactory) {
        super(executorService, rasterStyle);
        this.param = param;
        this.requestFactory = requestFactory;
    }

    @Override
    protected final TileCacheInformation createTileInformation(final MapBounds bounds, final Rectangle paintArea, final double dpi,
                                                               final boolean isFirstLayer) {
        return new WMTSTileCacheInfo(bounds, paintArea, dpi);
    }

    private final class WMTSTileCacheInfo extends TileCacheInformation {
        private Matrix matrix;

        public WMTSTileCacheInfo(final MapBounds bounds, final Rectangle paintArea, final double dpi) {
            super(bounds, paintArea, dpi, WMTSLayer.this.param);
            double diff = Double.POSITIVE_INFINITY;
            double targetResolution = bounds.getResolution(paintArea, dpi);
            for (Matrix m : WMTSLayer.this.param.matrices) {
                double delta = Math.abs(1 - m.resolution / targetResolution);
                if (delta < diff) {
                    diff = delta;
                    this.matrix = m;
                }
            }

            if (this.matrix == null) {
                throw new IllegalArgumentException("Unable to find a matrix that at the resolution: " + targetResolution);
            }
        }

        @Override
        @Nonnull
        public Dimension getTileSize() {
            int width = this.matrix.getTileWidth();
            int height = this.matrix.getTileHeight();
            return new Dimension(width, height);
        }

        @Nonnull
        @Override
        protected ReferencedEnvelope getTileCacheBounds() {
            double resolution = this.matrix.resolution;
            double minX = this.matrix.topLeftCorner[0];
            double minY = this.matrix.topLeftCorner[1] - (this.matrix.getTileHeight() * this.matrix.matrixSize[1] * resolution);
            double maxX = this.matrix.topLeftCorner[0] + (this.matrix.getTileWidth() * this.matrix.matrixSize[0] * resolution);
            double maxY = this.matrix.topLeftCorner[1];
            return new ReferencedEnvelope(minX, maxX, minY, maxY, bounds.getProjection());
        }

        @Override
        @Nonnull
        public ClientHttpRequest getTileRequest(final URI commonURI, final ReferencedEnvelope tileBounds,
                                                final Dimension tileSizeOnScreen) throws URISyntaxException {
            ReferencedEnvelope tileCacheBounds = getTileCacheBounds();
            double factor = 1 / (this.matrix.resolution * tileSizeOnScreen.width);
            int row = (int) Math.round((tileCacheBounds.getMaxY() - tileBounds.getMaxY()) * factor);
            int col = (int) Math.round((tileBounds.getMinY() - tileCacheBounds.getMinX()) * factor);
            URI uri;
            final WMTSLayerParam layerParam = WMTSLayer.this.param;
            if (RequestEncoding.REST == layerParam.requestEncoding) {
                String path = layerParam.baseURL;
                for (int i = 0; i < layerParam.dimensions.length; i++) {
                    String dimension = layerParam.dimensions[i];
                    final String value = layerParam.dimensionParams.getString(dimension.toUpperCase());
                    path = path.replace("{" + dimension + "}", value);
                }
                path = path.replace("{TileMatrixSet}", layerParam.matrixSet);
                path = path.replace("{TileMatrix}", this.matrix.identifier);
                path = path.replace("{TileRow}", String.valueOf(row));
                path = path.replace("{TileCol}", String.valueOf(col));

                uri = new URI(commonURI.getScheme(), commonURI.getUserInfo(), commonURI.getHost(), commonURI.getPort(),
                        path, commonURI.getQuery(), commonURI.getFragment());
            } else {
                String query = "SERVICE=WMTS";
                query += "&REQUEST=GetTile";
                query += "&VERSION=" + layerParam.version;
                query += "&LAYER=" + layerParam.layer;
                query += "&STYLE=" + layerParam.style;
                query += "&TILEMATRIXSET=" + layerParam.matrixSet;
                query += "&TILEMATRIX=" + this.matrix.identifier;
                query += "&TILEROW=" + row;
                query += "&TILECOL=" + col;
                query += "&FORMAT=" + layerParam.format;
                if (layerParam.dimensions != null) {
                    for (int i = 0; i < layerParam.dimensions.length; i++) {
                        String d = layerParam.dimensions[i];
                        query += "&" + d + "=" + layerParam.dimensionParams.getString(d.toUpperCase());
                    }
                }
                uri =  new URI(commonURI.getScheme(), commonURI.getUserInfo(), commonURI.getHost(), commonURI.getPort(),
                        commonURI.getPath(), query, commonURI.getFragment());
            }
            return null;
        }

        @Nullable
        @Override
        public BufferedImage getMissingTileImage() {
            return null;
        }

        @Override
        protected void addCommonQueryParams(final Multimap<String, String> result) {
            //no common params for this protocol.
        }
    }
}
