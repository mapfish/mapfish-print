package org.mapfish.print.map.image.wms;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.io.Closer;
import com.vividsolutions.jts.util.Assert;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.HttpRequestCache;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.image.AbstractSingleImageLayer;
import org.mapfish.print.map.style.json.ColorParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

/**
 * Wms layer.
 */
public final class WmsLayer extends AbstractSingleImageLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WmsLayer.class);
    private final WmsLayerParam params;
    private final MetricRegistry registry;
    private final Configuration configuration;
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
        super(executorService, styleSupplier, params);
        this.params = params;
        this.registry = registry;
        this.configuration = configuration;
    }

    @Override
    protected BufferedImage loadImage(
            @Nonnull final MfClientHttpRequestFactory requestFactory,
            @Nonnull final MapfishMapContext transformer) throws Throwable {

        final Closer closer = Closer.create();
        final String baseMetricName = WmsLayer.class.getName() + ".read." +
                this.imageRequest.getURI().getHost();
        try {
            final Timer.Context timerDownload = this.registry.timer(baseMetricName).time();
            LOGGER.info("Query the WMS image {}.", this.imageRequest.getURI());
            final ClientHttpResponse response = closer.register(this.imageRequest.execute());

            Assert.isTrue(response != null, "No response, see error above");
            Assert.equals(HttpStatus.OK, response.getStatusCode(), String.format("Http status code for %s " +
                    "was not OK.  It was: %s. The response message was: '%s'",
                    this.imageRequest.getURI(), response.getStatusCode(), response.getStatusText()));

            final List<String> contentType = response.getHeaders().get("Content-Type");
            if (contentType == null || contentType.size() != 1) {
                LOGGER.debug("The WMS image {} don't return a valid content type header.",
                        this.imageRequest.getURI());
            } else if (!contentType.get(0).startsWith("image/")) {
                byte[] data = new byte[response.getBody().available()];
                response.getBody().read(data);
                LOGGER.debug("We get a wrong WMS image for {}, content type: {}\nresult:\n{}",
                        this.imageRequest.getURI(), contentType.get(0), new String(data, "UTF-8"));
                this.registry.counter(baseMetricName + ".error").inc();
                return createErrorImage(transformer.getPaintArea());
            }

            final BufferedImage image = ImageIO.read(response.getBody());
            if (image == null) {
                LOGGER.warn("The WMS image {} is an image format that can be decoded",
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
        } finally {
            closer.close();
        }
    }

    private BufferedImage createErrorImage(final Rectangle area) {
        final BufferedImage bufferedImage = new BufferedImage(area.width, area.height, TYPE_INT_ARGB_PRE);
        final Graphics2D graphics = bufferedImage.createGraphics();
        try {
            graphics.setBackground(ColorParser.toColor(this.configuration.getTransparentTileErrorColor()));
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
        return this.params.useNativeAngle &&
                (this.params.serverType == WmsLayerParam.ServerType.MAPSERVER ||
                this.params.serverType == WmsLayerParam.ServerType.GEOSERVER);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.fromMimeType(this.params.imageFormat);
    }

    @Override
    public void cacheResources(
            @Nonnull final HttpRequestCache httpRequestCache,
            @Nonnull final MfClientHttpRequestFactory requestFactory,
            @Nonnull final MapfishMapContext transformer, @Nonnull final String jobId) {
        try {
            final MapfishMapContext layerTransformer = getLayerTransformer(transformer);

            final WmsLayerParam wmsLayerParam = this.params;
            final URI commonUri = new URI(wmsLayerParam.getBaseUrl());

            final Rectangle paintArea = layerTransformer.getPaintArea();
            final ReferencedEnvelope envelope = layerTransformer.getBounds().toReferencedEnvelope(paintArea);
            URI uri = WmsUtilities.makeWmsGetLayerRequest(wmsLayerParam, commonUri, paintArea.getSize(),
                    layerTransformer.getDPI(), layerTransformer.getRotation(), envelope);

            this.imageRequest = httpRequestCache.register(requestFactory, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
