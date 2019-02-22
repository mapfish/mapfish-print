package org.mapfish.print.map.image.wms;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.commons.io.IOUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.HttpRequestFetcher;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.image.AbstractSingleImageLayer;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

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

        final String baseMetricName = WmsLayer.class.getName() + ".read." +
                this.imageRequest.getURI().getHost();
        final Timer.Context timerDownload = this.registry.timer(baseMetricName).time();
        LOGGER.info("Query the WMS image {}.", this.imageRequest.getURI());
        try (ClientHttpResponse response = this.imageRequest.execute()) {

            Assert.isTrue(response != null, "No response, see error above");
            if (response.getStatusCode() != HttpStatus.OK) {
                final String message = String.format(
                        "Invalid status code for %s (%d!=%d). The response was: '%s'",
                        this.imageRequest.getURI(), response.getStatusCode().value(),
                        HttpStatus.OK.value(), response.getStatusText());
                this.registry.counter(baseMetricName + ".error").inc();
                if (getFailOnError()) {
                    throw new RuntimeException(message);
                } else {
                    LOGGER.info(message);
                    return createErrorImage(transformer.getPaintArea());
                }
            }
            Assert.equals(HttpStatus.OK, response.getStatusCode(),
                          String.format("Http status code for %s was not OK. The response message was: '%s'",
                                        this.imageRequest.getURI(), response.getStatusText()));

            final List<String> contentType = response.getHeaders().get("Content-Type");
            if (contentType == null || contentType.size() != 1) {
                LOGGER.debug("The WMS image {} didn't return a valid content type header.",
                             this.imageRequest.getURI());
            } else if (!contentType.get(0).startsWith("image/")) {
                final byte[] data;
                try (InputStream body = response.getBody()) {
                    data = IOUtils.toByteArray(body);
                }
                LOGGER.debug("We get a wrong WMS image for {}, content type: {}\nresult:\n{}",
                             this.imageRequest.getURI(), contentType.get(0),
                             new String(data, StandardCharsets.UTF_8));
                this.registry.counter(baseMetricName + ".error").inc();
                return createErrorImage(transformer.getPaintArea());
            }

            final BufferedImage image;
            try (InputStream body = response.getBody()) {
                image = ImageIO.read(body);
            }
            if (image == null) {
                LOGGER.warn("The WMS image {} is an image format that cannot be decoded",
                            this.imageRequest.getURI());
                this.registry.counter(baseMetricName + ".error").inc();
                return createErrorImage(transformer.getPaintArea());
            } else {
                timerDownload.stop();
            }
            return image;
        } catch (Throwable e) {
            this.registry.counter(baseMetricName + ".error").inc();
            throw e;
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
