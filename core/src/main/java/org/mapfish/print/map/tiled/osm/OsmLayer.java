package org.mapfish.print.map.tiled.osm;

import com.codahale.metrics.MetricRegistry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.URIUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
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
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;

/**
 * Strategy object for rendering Osm based layers.
 */
public final class OsmLayer extends AbstractTiledLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OsmLayer.class);
    private final OsmLayerParam param;

    /**
     * Constructor.
     *
     * @param forkJoinPool the thread pool for doing the rendering.
     * @param styleSupplier strategy for loading the style for this layer.
     * @param param the information needed to create OSM requests.
     * @param registry the metrics registry.
     * @param configuration the configuration.
     */
    public OsmLayer(
            @Nonnull final ForkJoinPool forkJoinPool,
            @Nonnull final StyleSupplier<GridCoverage2D> styleSupplier,
            @Nonnull final OsmLayerParam param,
            @Nonnull final MetricRegistry registry,
            @Nonnull final Configuration configuration) {
        super(forkJoinPool, styleSupplier, param, registry, configuration);
        this.param = param;
    }

    @Override
    protected TileCacheInformation createTileInformation(
            final MapBounds bounds, final Rectangle paintArea, final double dpi) {
        return new OsmTileCacheInformation(bounds, paintArea, dpi);
    }

    private final class OsmTileCacheInformation extends TileCacheInformation {
        private final double resolution;
        private final int resolutionIndex;

        public OsmTileCacheInformation(
                final MapBounds bounds, final Rectangle paintArea, final double dpi) {
            super(bounds, paintArea, dpi, OsmLayer.this.param);

            final double targetResolution = bounds.getScale(paintArea, dpi).getResolution();

            Double[] resolutions = OsmLayer.this.param.resolutions;
            int pos = resolutions.length - 1;
            double result = resolutions[pos];
            for (int i = resolutions.length - 1; i >= 0; --i) {
                double cur = resolutions[i];
                if (cur <= targetResolution * OsmLayer.this.param.resolutionTolerance) {
                    result = cur;
                    pos = i;
                    OsmLayer.this.imageBufferScaling = cur / targetResolution;
                }
            }

            this.resolution = result;
            this.resolutionIndex = pos;
        }

        @Nonnull
        @Override
        public ClientHttpRequest getTileRequest(final MfClientHttpRequestFactory httpRequestFactory,
                                                final String commonUrl,
                                                final ReferencedEnvelope tileBounds,
                                                final Dimension tileSizeOnScreen,
                                                final int column,
                                                final int row)
                throws IOException, URISyntaxException {

            final URI uri;
            if (commonUrl.contains("{x}") && commonUrl.contains("{z}")
                    && (commonUrl.contains("{y}") || commonUrl.contains("{-y}"))) {
                String url = commonUrl
                        .replace("{z}", String.format("%02d", this.resolutionIndex))
                        .replace("{x}", Integer.toString(column))
                        .replace("{y}", Integer.toString(row));
                if (commonUrl.contains("{-y}")) {
                    // {-y} is for  OSGeo TMS layers, see also: https://josm.openstreetmap.de/wiki/Maps#TileMapServicesTMS
                    url = url.replace("{-y}", Integer.toString(
                            (int) Math.pow(2, this.resolutionIndex) - 1 - row));
                }
                uri  = new URI(url);
            } else {
                StringBuilder path = new StringBuilder();
                if (!commonUrl.endsWith("/")) {
                    path.append('/');
                }
                path.append(String.format("%02d", this.resolutionIndex));
                path.append('/').append(column);
                path.append('/').append(row);
                path.append('.').append(OsmLayer.this.param.imageExtension);

                uri  = new URI(commonUrl + path.toString());
            }

            return httpRequestFactory.createRequest(
                    URIUtils.addParams(uri, OsmLayerParam.convertToMultiMap(OsmLayer.this.param.customParams),
                    URIUtils.getParameters(uri).keySet()), HttpMethod.GET);
        }

        @Override
        public double getResolution() {
            return this.resolution;
        }

        @Override
        public Double getLayerDpi() {
            return OsmLayer.this.param.dpi;
        }

        @Override
        public Dimension getTileSize() {
            return OsmLayer.this.param.getTileSize();
        }

        @Nonnull
        @Override
        protected ReferencedEnvelope getTileCacheBounds() {
            return new ReferencedEnvelope(OsmLayer.this.param.getMaxExtent(), this.bounds.getProjection());
        }
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.fromFileExtension(this.param.imageExtension);
    }
}
