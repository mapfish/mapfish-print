package org.mapfish.print.map.image.wms;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.io.Closer;
import com.vividsolutions.jts.util.Assert;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.HttpRequestCache;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.image.AbstractSingleImageLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
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
 */
public final class WmsLayer extends AbstractSingleImageLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WmsLayer.class);
    private final WmsLayerParam params;
    private final MetricRegistry registry;
    private ClientHttpRequest imageRequest;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param styleSupplier the style to use when drawing the constructed grid coverage on the map.
     * @param params the params from the request data.
     * @param registry the metrics registry.
     */
    protected WmsLayer(final ExecutorService executorService,
                       final StyleSupplier<GridCoverage2D> styleSupplier,
                       final WmsLayerParam params,
                       final MetricRegistry registry) {
        super(executorService, styleSupplier, params);
        this.params = params;
        this.registry = registry;
    }

    @Override
    protected BufferedImage loadImage(final MfClientHttpRequestFactory requestFactory,
                                      final MapfishMapContext transformer) throws Throwable {

        final Closer closer = Closer.create();
        final String baseMetricName = WmsLayer.class.getName() + ".read." +
                this.imageRequest.getURI().getHost();
        try {
            final Timer.Context timerDownload = this.registry.timer(baseMetricName).time();
            final ClientHttpResponse response = closer.register(this.imageRequest.execute());

            Assert.equals(HttpStatus.OK, response.getStatusCode(), "Http status code for " + this.imageRequest.getURI() + 
                    " was not OK.  It was: " + response .getStatusCode() + ".  The response message was: '" +
                    response.getStatusText() + "'");

            final BufferedImage image = ImageIO.read(response.getBody());
            if (image == null) {
                LOGGER.warn("The URI: " + this.imageRequest.getURI() + " is an image format that can be decoded");
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

    @Override
    public RenderType getRenderType() {
        return RenderType.fromMimeType(this.params.imageFormat);
    }
     
    @Override
    public void cacheResources(final HttpRequestCache httpRequestCache,
            final MfClientHttpRequestFactory requestFactory, final MapfishMapContext transformer) {
        try {
            final MapfishMapContext layerTransformer = getLayerTransformer(transformer);
            
            final WmsLayerParam wmsLayerParam = this.params;
            final URI commonUri = new URI(wmsLayerParam.getBaseUrl());
    
            final Rectangle paintArea = layerTransformer.getPaintArea();
            final ReferencedEnvelope envelope = layerTransformer.getBounds().toReferencedEnvelope(paintArea, 
                    layerTransformer.getDPI());
            URI uri = WmsUtilities.makeWmsGetLayerRequest(requestFactory, wmsLayerParam, commonUri, paintArea.getSize(),
                    layerTransformer.getDPI(), envelope);
            
            this.imageRequest = httpRequestCache.register(requestFactory, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
