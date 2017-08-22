package org.mapfish.print.map.tiled.wms;

import com.codahale.metrics.MetricRegistry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Configuration;
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
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;

import static org.mapfish.print.map.image.wms.WmsUtilities.makeWmsGetLayerRequest;

/**
 * Strategy object for rendering WMS based layers .
 */
public final class TiledWmsLayer extends AbstractTiledLayer {
    private final TiledWmsLayerParam param;

    /**
     * Constructor.
     *
     * @param forkJoinPool the thread pool for doing the rendering.
     * @param styleSupplier strategy for loading the style for this layer.
     * @param param the information needed to create WMS requests.
     * @param registry the metrics registry.
     * @param configuration the configuration.
     */
    public TiledWmsLayer(
            @Nonnull final ForkJoinPool forkJoinPool,
            @Nonnull final StyleSupplier<GridCoverage2D> styleSupplier,
            @Nonnull final TiledWmsLayerParam param,
            @Nonnull final MetricRegistry registry,
            @Nonnull final Configuration configuration) {
        super(forkJoinPool, styleSupplier, param, registry, configuration);
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
            final MapBounds bounds, final Rectangle paintArea, final double dpi) {
        return new WmsTileCacheInformation(bounds, paintArea, dpi);
    }

    private final class WmsTileCacheInformation extends TileCacheInformation {

        public WmsTileCacheInformation(
                final MapBounds bounds, final Rectangle paintArea, final double dpi) {
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

            URI uri = makeWmsGetLayerRequest(TiledWmsLayer.this.param, new URI(commonUrl),
                    tileSizeOnScreen, this.dpi, 0.0, tileBounds);
            return httpRequestFactory.createRequest(uri, HttpMethod.GET);
        }

        @Override
        public double getResolution() {
            return WmsTileCacheInformation.this.bounds.getScale(WmsTileCacheInformation.this.paintArea, dpi)
                   .getResolution();
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
                    this.bounds.toReferencedEnvelope(paintArea),
                    this.bounds.getProjection());
        }
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.fromMimeType(this.param.imageFormat);
    }
}
