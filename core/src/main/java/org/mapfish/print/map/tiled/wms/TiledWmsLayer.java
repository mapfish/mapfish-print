package org.mapfish.print.map.tiled.wms;

import com.codahale.metrics.MetricRegistry;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.tiled.AbstractTiledLayer;
import org.mapfish.print.map.tiled.TileCacheInformation;
import org.opengis.referencing.FactoryException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;

import static org.mapfish.print.map.image.wms.WmsUtilities.makeWmsGetLayerRequest;

/**
 * Strategy object for rendering WMS based layers .
 *
 * @author Stéphane Brunner on 22/07/2014.
 */
public final class TiledWmsLayer extends AbstractTiledLayer {
    private final TiledWmsLayerParam param;

    /**
     * Constructor.
     *
     * @param forkJoinPool  the thread pool for doing the rendering.
     * @param requestForkJoinPool the thread pool for making tile/image requests.
     * @param styleSupplier strategy for loading the style for this layer
     * @param param         the information needed to create WMS requests.
     * @param registry      the metrics registry.
     */
    public TiledWmsLayer(
            final ForkJoinPool forkJoinPool,
            final ForkJoinPool requestForkJoinPool,
            final StyleSupplier<GridCoverage2D> styleSupplier,
            final TiledWmsLayerParam param,
            final MetricRegistry registry) {
        super(forkJoinPool, requestForkJoinPool, styleSupplier, param, registry);
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
                final MfClientHttpRequestFactory httpRequestFactory,
                final String commonUrl,
                final ReferencedEnvelope tileBounds,
                final Dimension tileSizeOnScreen,
                final int column,
                final int row)
                throws IOException, URISyntaxException, FactoryException {

            URI uri =
                    makeWmsGetLayerRequest(httpRequestFactory, TiledWmsLayer.this.param,
                            new URI(commonUrl), tileSizeOnScreen, this.dpi, tileBounds);
            return httpRequestFactory.createRequest(uri, HttpMethod.GET);
        }

        @Override
        public double getResolution() {
            return WmsTileCacheInformation.this.bounds.getScaleDenominator(
                    WmsTileCacheInformation.this.paintArea, dpi).toResolution(WmsTileCacheInformation.this.bounds.getProjection(), dpi);
        }

        @Override
        public Double getLayerDpi() {
            return this.dpi;
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
