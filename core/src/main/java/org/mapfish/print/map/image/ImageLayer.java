package org.mapfish.print.map.image;

import com.codahale.metrics.MetricRegistry;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.parser.HasDefaultValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

/**
 * <p>Reads a image file from an URL.</p>
 *
 * @author MaxComse on 11/08/16.
 */
public final class ImageLayer extends AbstractSingleImageLayer {
    private final ImageParam params;
    private final StyleSupplier<GridCoverage2D> styleSupplier;
    private final ExecutorService executorService;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param styleSupplier the style to use when drawing the constructed grid coverage on the map.
     * @param params the params from the request data.
     * @param configuration the configuration.
     * @param registry the metrics object.
     */
    protected ImageLayer(
            @Nonnull final ExecutorService executorService,
            @Nonnull final StyleSupplier<GridCoverage2D> styleSupplier,
            @Nonnull final ImageParam params,
            @Nonnull final Configuration configuration,
            @Nonnull final MetricRegistry registry) {
        super(executorService, styleSupplier, params, registry, configuration);
        this.params = params;
        this.styleSupplier = styleSupplier;
        this.executorService = executorService;
    }

    @Override
    protected BufferedImage loadImage(
            final MfClientHttpRequestFactory requestFactory,
            final MapfishMapContext transformer) throws Throwable {
        final ImageParam layerParam = this.params;
        final URI commonUri = new URI(layerParam.getBaseUrl());

        final Rectangle paintArea = transformer.getPaintArea();

        final ReferencedEnvelope envelope = transformer.getBounds().toReferencedEnvelope(paintArea);
        final CoordinateReferenceSystem mapProjection = envelope.getCoordinateReferenceSystem();

        final BufferedImage bufferedImage = new BufferedImage(paintArea.width, paintArea.height,
                                                              TYPE_INT_ARGB_PRE);
        final Graphics2D graphics = bufferedImage.createGraphics();
        final MapBounds bounds = transformer.getBounds();
        final MapContent content = new MapContent();
        final ClientHttpRequest request = requestFactory.createRequest(commonUri, HttpMethod.GET);
        final BufferedImage image = fetchImage(request, transformer);

        try {
            GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
            GeneralEnvelope gridEnvelope = new GeneralEnvelope(mapProjection);

            gridEnvelope.setEnvelope(layerParam.extent);
            GridCoverage2D coverage = factory.create(layerParam.getBaseUrl(), image, gridEnvelope,
                                                     null, null, null);
            Style style = this.styleSupplier.load(requestFactory, coverage);
            GridCoverageLayer l = new GridCoverageLayer(coverage, style);

            content.addLayers(Collections.singletonList(l));

            StreamingRenderer renderer = new StreamingRenderer();

            RenderingHints hints = new RenderingHints(Collections.emptyMap());
            hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION,
                      RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            graphics.addRenderingHints(hints);
            renderer.setJava2DHints(hints);
            Map<String, Object> renderHints = new HashMap<>();
            if (transformer.isForceLongitudeFirst() != null) {
                renderHints.put(StreamingRenderer.FORCE_EPSG_AXIS_ORDER_KEY,
                                transformer.isForceLongitudeFirst());
            }
            renderer.setRendererHints(renderHints);

            renderer.setMapContent(content);
            renderer.setThreadPool(this.executorService);

            final ReferencedEnvelope mapArea = bounds.toReferencedEnvelope(paintArea);
            renderer.paint(graphics, paintArea, mapArea);
            return bufferedImage;
        } finally {
            graphics.dispose();
            content.dispose();
        }
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.UNKNOWN;
    }

    /**
     * <p>Renders an image as layer.</p>
     * <p>Type: <code>image</code></p>
     */
    public static final class ImageLayerPlugin extends AbstractGridCoverageLayerPlugin
            implements MapLayerFactoryPlugin<ImageParam> {
        private static final String TYPE = "image";
        @Autowired
        private ForkJoinPool forkJoinPool;
        @Autowired
        private MetricRegistry metricRegistry;

        @Override
        public Set<String> getTypeNames() {
            return Collections.singleton(TYPE);
        }

        @Override
        public ImageParam createParameter() {
            return new ImageParam();
        }

        @Nonnull
        @Override
        public ImageLayer parse(
                @Nonnull final Template template,
                @Nonnull final ImageParam layerData) {
            String styleRef = layerData.style;
            return new ImageLayer(this.forkJoinPool,
                                  super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                                  layerData, template.getConfiguration(), metricRegistry);
        }
    }

    /**
     * The parameters for reading an image file, either from the server or from a URL.
     */
    public static final class ImageParam extends AbstractLayerParams {

        private static final int NUMBER_OF_EXTENT_COORDS = 4;
        /**
         * The base URL for the image file.  Used for making request.
         */
        public String baseURL;

        /**
         * The extent of the image.  Used for placing image on map.
         */
        public double[] extent;

        /**
         * The styles to apply to the image.
         */
        @HasDefaultValue
        public String style = Constants.Style.Raster.NAME;


        /**
         * Validate the properties have the correct values.
         */
        public void postConstruct() {
            Assert.equals(NUMBER_OF_EXTENT_COORDS, this.extent.length,
                          "maxExtent must have exactly 4 elements to the array.  Was: " +
                                  Arrays.toString(this.extent));
        }

        public String getBaseUrl() {
            return this.baseURL;
        }
    }
}
