package org.mapfish.print.map.geotools;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.util.Assert;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleVisitor;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.FloatingPointUtil;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.mapfish.print.Constants.OPACITY_PRECISION;

/**
 * @author Jesse on 3/26/14.
 */
public abstract class AbstractGeotoolsLayer implements MapLayer {

    private final ExecutorService executorService;
    private final AbstractLayerParams params;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param params          the parameters for this layer
     */
    protected AbstractGeotoolsLayer(final ExecutorService executorService, final AbstractLayerParams params) {
        this.executorService = executorService;
        this.params = params;
    }


    @Override
    public final Optional<MapLayer> tryAddLayer(final MapLayer newLayer) {
        return Optional.absent();
    }

    @Override
    public final void render(final Graphics2D graphics2D,
                             final MfClientHttpRequestFactory clientHttpRequestFactory,
                             final MapfishMapContext transformer,
                             final boolean isFirstLayer) {
        Rectangle paintArea = new Rectangle(transformer.getMapSize());
        MapBounds bounds = transformer.getBounds();

        MapfishMapContext layerTransformer = transformer;
        if (!FloatingPointUtil.equals(transformer.getRotation(), 0.0) && !this.supportsNativeRotation()) {
            // if a rotation is set and the rotation can not be handled natively
            // by the layer, we have to adjust the bounds and map size
            paintArea = new Rectangle(transformer.getRotatedMapSize());
            bounds = transformer.getRotatedBounds();
            graphics2D.setTransform(transformer.getTransform());
            Dimension mapSize = new Dimension(paintArea.width, paintArea.height);
            layerTransformer = new MapfishMapContext(transformer, bounds, mapSize, transformer.getRotation(), transformer.getDPI(),
                    transformer.getRequestorDPI(), transformer.isForceLongitudeFirst(), transformer.isDpiSensitiveStyle());
        }


        MapContent content = new MapContent();
        try {
            List<? extends Layer> layers = getLayers(clientHttpRequestFactory, layerTransformer, isFirstLayer);
            applyTransparency(layers);

            content.addLayers(layers);

            StreamingRenderer renderer = new StreamingRenderer();

            RenderingHints hints = new RenderingHints(Collections.<RenderingHints.Key, Object>emptyMap());
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
            Map<String, Object> renderHints = Maps.newHashMap();
            if (transformer.isForceLongitudeFirst() != null) {
                renderHints.put(StreamingRenderer.FORCE_EPSG_AXIS_ORDER_KEY, transformer.isForceLongitudeFirst());
            }
            renderer.setRendererHints(renderHints);

            renderer.setMapContent(content);
            renderer.setThreadPool(this.executorService);

            final ReferencedEnvelope mapArea = bounds.toReferencedEnvelope(paintArea, transformer.getDPI());
            renderer.paint(graphics2D, paintArea, mapArea);
        } catch (Exception e) {
            throw ExceptionUtils.getRuntimeException(e);
        } finally {
            content.dispose();
        }
    }

    private void applyTransparency(final List<? extends Layer> layers) {
        final double opacity = this.params.opacity;
        Assert.isTrue(opacity > -OPACITY_PRECISION && opacity < (1 + OPACITY_PRECISION),
                "Opacity of " + this + " is an illegal value: " + opacity);

        if (1.0 - opacity > OPACITY_PRECISION) {
            StyleVisitor visitor = new OpacitySettingStyleVisitor(opacity);

            for (Layer layer : layers) {
                final Style style = layer.getStyle();
                style.accept(visitor);
            }
        }
    }


    /**
     * Get the {@link org.geotools.data.DataStore} object that contains the data for this layer.
     *
     * @param httpRequestFactory the factory for making http requests
     * @param transformer        the map transformer
     * @param isFirstLayer       true indicates this layer is the first layer in the map (the first layer drawn, ie the base layer)
     */
    protected abstract List<? extends Layer> getLayers(MfClientHttpRequestFactory httpRequestFactory,
                                                       MapfishMapContext transformer,
                                                       final boolean isFirstLayer) throws Exception;

    //CHECKSTYLE:OFF: DesignForExtension - Set a default value for all sub classes.
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
}
