package org.mapfish.print.map.geotools;

import org.geotools.data.DataStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleVisitor;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.FloatingPointUtil;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.HttpRequestFetcher;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.processor.Processor;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.mapfish.print.Constants.OPACITY_PRECISION;

/**
 * The AbstractGeotoolsLayer class.
 */
public abstract class AbstractGeotoolsLayer implements MapLayer {

    private final ExecutorService executorService;
    private final AbstractLayerParams params;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param params the parameters for this layer
     */
    protected AbstractGeotoolsLayer(final ExecutorService executorService, final AbstractLayerParams params) {
        this.executorService = executorService;
        this.params = params;
    }

    /**
     * Copy constructor.
     *
     * @param other The source.
     */
    public AbstractGeotoolsLayer(final AbstractGeotoolsLayer other) {
        this.executorService = other.executorService;
        this.params = other.params;
    }


    @Override
    public final Optional<MapLayer> tryAddLayer(final MapLayer newLayer) {
        return Optional.empty();
    }

    @Override
    public void prepareRender(final MapfishMapContext transformer) {

    }

    @Override
    public final void render(
            final Graphics2D graphics2D,
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final MapfishMapContext transformer, final Processor.ExecutionContext context) {
        java.awt.geom.AffineTransform originalTransform = graphics2D.getTransform();

        MapfishMapContext layerTransformer = getLayerTransformer(transformer);

        if (!FloatingPointUtil.equals(transformer.getRotation(), 0.0) && !this.supportsNativeRotation()) {
            graphics2D.setTransform(transformer.getTransform());
        }

        Rectangle paintArea = new Rectangle(layerTransformer.getMapSize());
        MapContent content = new MapContent();
        try {
            List<? extends Layer> layers = getLayers(clientHttpRequestFactory, layerTransformer, context);
            applyTransparency(layers);

            content.addLayers(layers);

            StreamingRenderer renderer = new StreamingRenderer();

            RenderingHints hints = new RenderingHints(Collections.emptyMap());
            hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
                                         RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
            hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                                         RenderingHints.VALUE_ANTIALIAS_ON));
            hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING,
                                         RenderingHints.VALUE_COLOR_RENDER_QUALITY));
            hints.add(new RenderingHints(RenderingHints.KEY_DITHERING,
                                         RenderingHints.VALUE_DITHER_ENABLE));
            hints.add(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS,
                                         RenderingHints.VALUE_FRACTIONALMETRICS_ON));
            hints.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                                         RenderingHints.VALUE_INTERPOLATION_BICUBIC));
            hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,
                                         RenderingHints.VALUE_RENDER_QUALITY));
            hints.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL,
                                         RenderingHints.VALUE_STROKE_PURE));
            hints.add(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                                         RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

            graphics2D.addRenderingHints(hints);
            renderer.setJava2DHints(hints);
            Map<String, Object> renderHints = new HashMap<>();
            if (transformer.isForceLongitudeFirst() != null) {
                renderHints.put(StreamingRenderer.FORCE_EPSG_AXIS_ORDER_KEY,
                                transformer.isForceLongitudeFirst());
            }
            renderer.setRendererHints(renderHints);

            renderer.setMapContent(content);
            renderer.setThreadPool(this.executorService);

            final ReferencedEnvelope mapArea = layerTransformer.getBounds().toReferencedEnvelope(paintArea);
            renderer.paint(graphics2D, paintArea, mapArea);
            graphics2D.setTransform(originalTransform);
        } catch (Exception e) {
            throw ExceptionUtils.getRuntimeException(e);
        } finally {
            content.dispose();
        }
    }

    public double getOpacity() {
        return this.params.opacity;
    }


    private void applyTransparency(final List<? extends Layer> layers) {
        final double opacity = this.params.opacity;
        Assert.isTrue(opacity > -OPACITY_PRECISION && opacity < (1 + OPACITY_PRECISION),
                      "Opacity of " + this + " is an illegal value: " + opacity);

        if (1.0 - opacity > OPACITY_PRECISION) {
            StyleVisitor visitor = new OpacitySettingStyleVisitor(opacity);

            for (Layer layer: layers) {
                final Style style = layer.getStyle();
                style.accept(visitor);
            }
        }
    }


    /**
     * Get the {@link DataStore} object that contains the data for this layer.
     *
     * @param httpRequestFactory the factory for making http requests
     * @param transformer the map transformer
     * @param context the job ID
     */
    protected abstract List<? extends Layer> getLayers(
            MfClientHttpRequestFactory httpRequestFactory,
            MapfishMapContext transformer, Processor.ExecutionContext context) throws Exception;

    @Override
    public boolean supportsNativeRotation() {
        return false;
    }
    //CHECKSTYLE:ON

    public final String getName() {
        return this.params.name;
    }

    public final boolean getFailOnError() {
        return this.params.failOnError;
    }

    /**
     * If the layer transformer has not been prepared yet, do it.
     *
     * @param transformer the transformer
     */
    protected final MapfishMapContext getLayerTransformer(final MapfishMapContext transformer) {
        MapfishMapContext layerTransformer = transformer;

        if (!FloatingPointUtil.equals(transformer.getRotation(), 0.0) && !this.supportsNativeRotation()) {
            // if a rotation is set and the rotation can not be handled natively
            // by the layer, we have to adjust the bounds and map size
            layerTransformer = new MapfishMapContext(
                    transformer,
                    transformer.getRotatedBoundsAdjustedForPreciseRotatedMapSize(),
                    transformer.getRotatedMapSize(),
                    0,
                    transformer.getDPI(),
                    transformer.isForceLongitudeFirst(),
                    transformer.isDpiSensitiveStyle());
        }

        return layerTransformer;
    }

    @Override
    public void prefetchResources(
            final HttpRequestFetcher httpRequestFetcher,
            final MfClientHttpRequestFactory clientHttpRequestFactory, final MapfishMapContext transformer,
            final Processor.ExecutionContext context) {
    }
}
