package org.mapfish.print.map.image.wms;

import com.codahale.metrics.MetricRegistry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.HttpRequestFetcher;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.image.AbstractSingleImageLayer;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequest;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

/**
 * Wms layer.
 */
public final class WmsLayer extends AbstractSingleImageLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WmsLayer.class);
    private final WmsLayerParam params;
    private ClientHttpRequest imageRequest;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param styleSupplier the style to use when drawing the constructed grid coverage on the map.
     * @param params the params from the request data.
     * @param registry the metrics registry.
     * @param configuration the configuration.
     */
    protected WmsLayer(
            @Nonnull final ExecutorService executorService,
            @Nonnull final StyleSupplier<GridCoverage2D> styleSupplier,
            @Nonnull final WmsLayerParam params,
            @Nonnull final MetricRegistry registry,
            @Nonnull final Configuration configuration) {
        super(executorService, styleSupplier, params, registry, configuration);
        this.params = params;
    }

    @Override
    protected BufferedImage loadImage(
            @Nonnull final MfClientHttpRequestFactory requestFactory,
            @Nonnull final MapfishMapContext transformer) throws Throwable {

        LOGGER.info("Query the WMS image {}.", this.imageRequest.getURI());
        return fetchImage(imageRequest, transformer);
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
     * If supported by the WMS server, a parameter "angle" can be set on "customParams" or "mergeableParams".
     * In this case the rotation will be done natively by the WMS.
     */
    @Override
    public boolean supportsNativeRotation() {
        return this.params.useNativeAngle &&
                (this.params.serverType == WmsLayerParam.ServerType.MAPSERVER ||
                        this.params.serverType == WmsLayerParam.ServerType.GEOSERVER);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.fromMimeType(this.params.imageFormat);
    }

    @Override
    public void prefetchResources(
            @Nonnull final HttpRequestFetcher httpRequestFetcher,
            @Nonnull final MfClientHttpRequestFactory requestFactory,
            @Nonnull final MapfishMapContext transformer, @Nonnull final Processor.ExecutionContext context) {
        try {
            final MapfishMapContext layerTransformer = getLayerTransformer(transformer);

            final WmsLayerParam wmsLayerParam = this.params;
            final URI commonUri = new URI(wmsLayerParam.getBaseUrl());

            final Rectangle paintArea = layerTransformer.getPaintArea();
            final ReferencedEnvelope envelope = layerTransformer.getBounds().toReferencedEnvelope(paintArea);
            URI uri = WmsUtilities.makeWmsGetLayerRequest(wmsLayerParam, commonUri, paintArea.getSize(),
                                                          layerTransformer.getDPI(),
                                                          layerTransformer.getRotation(), envelope);

            this.imageRequest = httpRequestFetcher.register(
                    WmsUtilities.createWmsRequest(requestFactory, uri, this.params.method));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
