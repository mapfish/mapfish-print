package org.mapfish.print.map.tiled.wmts;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.URIUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.Scale;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.tiled.AbstractTiledLayer;
import org.mapfish.print.map.tiled.TileCacheInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;


/**
 * Class for loading data from a WMTS.
 *
 * @author Jesse on 4/3/14.
 */
public class WMTSLayer extends AbstractTiledLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WMTSLayer.class);
    private final WMTSLayerParam param;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param styleSupplier   strategy for loading the style for this layer
     * @param param           the information needed to create WMTS requests.
     */
    protected WMTSLayer(final ForkJoinPool executorService,
                        final StyleSupplier<GridCoverage2D> styleSupplier,
                        final WMTSLayerParam param) {
        super(executorService, styleSupplier, param);
        this.param = param;
    }

    @Override
    protected final TileCacheInformation createTileInformation(final MapBounds bounds, final Rectangle paintArea, final double dpi,
                                                               final boolean isFirstLayer) {
        return new WMTSTileCacheInfo(bounds, paintArea, dpi);
    }

    @VisibleForTesting
    final class WMTSTileCacheInfo extends TileCacheInformation {
        private Matrix matrix;

        public WMTSTileCacheInfo(final MapBounds bounds, final Rectangle paintArea, final double dpi) {
            super(bounds, paintArea, dpi, WMTSLayer.this.param);
            double diff = Double.POSITIVE_INFINITY;
            final double targetScale = bounds.getScaleDenominator(paintArea, dpi).getDenominator();
            for (Matrix m : WMTSLayer.this.param.matrices) {
                double delta = Math.abs(m.scaleDenominator - targetScale);
                if (delta < diff) {
                    diff = delta;
                    this.matrix = m;
                }
            }

            if (this.matrix == null) {
                throw new IllegalArgumentException("Unable to find a matrix that at the scale: " + targetScale);
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
            double scaleDenominator = new Scale(this.matrix.scaleDenominator).toResolution(this.bounds.getProjection(), getLayerDpi());
            double minX = this.matrix.topLeftCorner[0];
            double tileHeight = this.matrix.getTileHeight();
            double numYTiles = this.matrix.matrixSize[1];
            double minY = this.matrix.topLeftCorner[1] - (tileHeight * numYTiles * scaleDenominator);
            double tileWidth = this.matrix.getTileWidth();
            double numXTiles = this.matrix.matrixSize[0];
            double maxX = this.matrix.topLeftCorner[0] + (tileWidth * numXTiles * scaleDenominator);
            double maxY = this.matrix.topLeftCorner[1];
            return new ReferencedEnvelope(minX, maxX, minY, maxY, bounds.getProjection());
        }

        @Override
        @Nonnull
        public ClientHttpRequest getTileRequest(final MfClientHttpRequestFactory httpRequestFactory,
                                                final String commonUrl,
                                                final ReferencedEnvelope tileBounds,
                                                final Dimension tileSizeOnScreen,
                                                final int column,
                                                final int row)
                throws URISyntaxException, IOException {
            URI uri;
            final WMTSLayerParam layerParam = WMTSLayer.this.param;
            if (RequestEncoding.REST == layerParam.requestEncoding) {
                uri = createRestURI(commonUrl, this.matrix.identifier, row, column, layerParam);
            } else {
                URI commonUri = new URI(commonUrl);
                uri = createKVPUri(commonUri, row, column, layerParam);
            }
            return httpRequestFactory.createRequest(uri, HttpMethod.GET);
        }

        private URI createKVPUri(final URI commonURI, final int row, final int col,
                                 final WMTSLayerParam layerParam) throws URISyntaxException {
            URI uri;
            final Multimap<String, String> queryParams = URIUtils.getParameters(commonURI);
            queryParams.put("SERVICE", "WMTS");
            queryParams.put("REQUEST", "GetTile");
            queryParams.put("VERSION", layerParam.version);
            queryParams.put("LAYER", layerParam.layer);
            queryParams.put("STYLE", layerParam.style);
            queryParams.put("TILEMATRIXSET", layerParam.matrixSet);
            queryParams.put("TILEMATRIX", this.matrix.identifier);
            queryParams.put("TILEROW", String.valueOf(row));
            queryParams.put("TILECOL", String.valueOf(col));
            if (layerParam.imageFormat.indexOf('/') > 0) {
                queryParams.put("FORMAT", layerParam.imageFormat);
            } else {
                LOGGER.warn("The format should be a mime type");
                queryParams.put("FORMAT", "image/" + layerParam.imageFormat);
            }
            if (layerParam.dimensions != null) {
                for (int i = 0; i < layerParam.dimensions.length; i++) {
                    String d = layerParam.dimensions[i];
                    String dimensionValue = layerParam.dimensionParams.optString(d);
                    if (dimensionValue == null) {
                        dimensionValue = layerParam.dimensionParams.getString(d.toUpperCase());
                    }
                    queryParams.put(d, dimensionValue);
                }
            }
            uri = URIUtils.setQueryParams(commonURI, queryParams);
            return uri;
        }

        @Override
        public Scale getScale() {
            return new Scale(this.matrix.scaleDenominator);
        }

        @Override
        public Double getLayerDpi() {
            return WMTSLayer.this.param.dpi;
        }
    }

    /**
     * Prepare the baseURL to make a request.
     *
     * @param commonURL Base URL
     * @param matrixId matrixId
     * @param row row
     * @param col cold
     * @param layerParam layerParam
     */
    public static URI createRestURI(final String commonURL, final String matrixId, final int row, final int col,
                              final WMTSLayerParam layerParam) throws URISyntaxException {
        String path = layerParam.baseURL;
        if (layerParam.dimensions != null) {
            for (int i = 0; i < layerParam.dimensions.length; i++) {
                String dimension = layerParam.dimensions[i];
                String value = layerParam.dimensionParams.optString(dimension);
                if (value == null) {
                    value = layerParam.dimensionParams.getString(dimension.toUpperCase());
                }
                path = path.replace("{" + dimension + "}", value);
            }
        }
        path = path.replace("{TileMatrixSet}", layerParam.matrixSet);
        path = path.replace("{TileMatrix}", matrixId);
        path = path.replace("{TileRow}", String.valueOf(row));
        path = path.replace("{TileCol}", String.valueOf(col));
        path = path.replace("{style}", layerParam.style);
        path = path.replace("{Layer}", layerParam.layer);

        return new URI(path);
    }
}
