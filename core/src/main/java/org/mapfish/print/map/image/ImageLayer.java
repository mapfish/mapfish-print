package org.mapfish.print.map.image;

import com.google.common.collect.Maps;
import com.google.common.io.Closer;
import com.vividsolutions.jts.util.Assert;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
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
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.parser.HasDefaultValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

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
    private final Configuration configuration;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param styleSupplier the style to use when drawing the constructed grid coverage on the map.
     * @param params the params from the request data.
     * @param configuration the configuration.
     */
    protected ImageLayer(
            @Nonnull final ExecutorService executorService,
            @Nonnull final StyleSupplier<GridCoverage2D> styleSupplier,
            @Nonnull final ImageParam params,
            @Nonnull final Configuration configuration) {
        super(executorService, styleSupplier, params);
        this.params = params;
        this.styleSupplier = styleSupplier;
        this.executorService = executorService;
        this.configuration = configuration;
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
                    layerData, template.getConfiguration());
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
         * @throws URISyntaxException
         */
        public void postConstruct() throws URISyntaxException {
            Assert.equals(NUMBER_OF_EXTENT_COORDS, this.extent.length,
                    "maxExtent must have exactly 4 elements to the array.  Was: " +
                            Arrays.toString(this.extent));
        }

        public String getBaseUrl() {
            return this.baseURL;
        }
    }

    @Override
    protected BufferedImage loadImage(final MfClientHttpRequestFactory requestFactory,
              final MapfishMapContext transformer) throws Throwable {
        final ImageParam layerParam = this.params;
        final URI commonUri = new URI(layerParam.getBaseUrl());

        final Double extentMinX = layerParam.extent[0];
        final Double extentMinY = layerParam.extent[1];
        final Double extentMaxX = layerParam.extent[2];
        final Double extentMaxY = layerParam.extent[3];
        final Rectangle paintArea = transformer.getPaintArea();

        final ReferencedEnvelope envelope = transformer.getBounds().toReferencedEnvelope(paintArea);
        final CoordinateReferenceSystem mapProjection = envelope.getCoordinateReferenceSystem();

        Closer closer = Closer.create();
        final BufferedImage bufferedImage = new BufferedImage(paintArea.width, paintArea.height,
                TYPE_INT_ARGB_PRE);
        final Graphics2D graphics = bufferedImage.createGraphics();
        MapBounds bounds = transformer.getBounds();
        MapContent content = new MapContent();
        try {
            final ClientHttpRequest request = requestFactory.createRequest(commonUri, HttpMethod.GET);
            final ClientHttpResponse httpResponse = closer.register(request.execute());
            final BufferedImage image = ImageIO.read(httpResponse.getBody());
            if (image == null) {
                return createErrorImage(paintArea);
            }

            GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
            GeneralEnvelope gridEnvelope = new GeneralEnvelope(mapProjection);

            gridEnvelope.setEnvelope(extentMinX, extentMinY, extentMaxX, extentMaxY);
            GridCoverage2D coverage = factory.create(layerParam.getBaseUrl(), image, gridEnvelope,
                    null, null, null);
            Style style = this.styleSupplier.load(requestFactory, coverage);
            GridCoverageLayer l = new GridCoverageLayer(coverage, style);

            content.addLayers(Collections.singletonList(l));

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

            graphics.addRenderingHints(hints);
            renderer.setJava2DHints(hints);
            Map<String, Object> renderHints = Maps.newHashMap();
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

    @Override
    public RenderType getRenderType() {
        return RenderType.UNKNOWN;
    }
}
