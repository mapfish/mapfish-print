package org.mapfish.print.map.image;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

import com.codahale.metrics.MetricRegistry;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.parser.HasDefaultValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

/**
 * Reads a image file from an URL.
 *
 * @author MaxComse on 11/08/16.
 */
public final class ImageLayer extends AbstractSingleImageLayer {
  private final ImageParam params;
  private final boolean failOnError;
  private final StyleSupplier<GridCoverage2D> styleSupplier;
  private final ExecutorService executorService;
  private final RenderType renderType;
  private double imageBufferScaling;
  private BufferedImage image;
  private boolean imageLoadError = false;
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageLayer.class);

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
    this.failOnError = params.failOnError;
    params.failOnError = true;
    this.styleSupplier = styleSupplier;
    this.executorService = executorService;
    this.renderType = RenderType.fromMimeType(params.imageFormat);
  }

  @Override
  protected BufferedImage loadImage(
      final MfClientHttpRequestFactory requestFactory, final MapfishMapContext transformer) {
    final ReferencedEnvelope envelopeOrig =
        transformer.getBounds().toReferencedEnvelope(transformer.getPaintArea());
    final Rectangle paintArea;
    if (imageLoadError) {
      paintArea = transformer.getPaintArea();
    } else {
      paintArea = calculateNewBounds(image, envelopeOrig);
    }
    final ReferencedEnvelope envelope = transformer.getBounds().toReferencedEnvelope(paintArea);
    final BufferedImage bufferedImage =
        new BufferedImage(paintArea.width, paintArea.height, TYPE_INT_ARGB_PRE);
    final Graphics2D graphics = bufferedImage.createGraphics();
    final MapContent content = new MapContent();

    try {
      GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
      final CoordinateReferenceSystem mapProjection = envelope.getCoordinateReferenceSystem();
      GeneralBounds gridEnvelope = new GeneralBounds(mapProjection);

      gridEnvelope.setEnvelope(this.params.extent);
      GridCoverage2D coverage =
          factory.create(this.params.getBaseUrl(), image, gridEnvelope, null, null, null);
      Style style = this.styleSupplier.load(requestFactory, coverage);

      content.addLayers(Collections.singletonList(new GridCoverageLayer(coverage, style)));

      StreamingRenderer renderer = new StreamingRenderer();

      RenderingHints hints = new RenderingHints(Collections.emptyMap());
      hints.put(
          RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
      hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
      hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      hints.put(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      graphics.addRenderingHints(hints);
      renderer.setJava2DHints(hints);
      Map<String, Object> renderHints = new HashMap<>();
      if (transformer.isForceLongitudeFirst() != null) {
        renderHints.put(
            StreamingRenderer.FORCE_EPSG_AXIS_ORDER_KEY, transformer.isForceLongitudeFirst());
      }
      renderer.setRendererHints(renderHints);

      renderer.setMapContent(content);
      renderer.setThreadPool(this.executorService);

      renderer.paint(graphics, paintArea, envelope);
      return bufferedImage;
    } finally {
      graphics.dispose();
      content.dispose();
    }
  }

  private Rectangle calculateNewBounds(
      final BufferedImage image, final ReferencedEnvelope envelope) {
    double w = (image.getWidth() / (params.extent[2] - params.extent[0])) * envelope.getWidth();
    double h = (image.getHeight() / (params.extent[3] - params.extent[1])) * envelope.getHeight();
    return new Rectangle(Math.toIntExact(Math.round(w)), Math.toIntExact(Math.round(h)));
  }

  @Override
  public RenderType getRenderType() {
    return this.renderType;
  }

  @Override
  public double getImageBufferScaling() {
    return imageBufferScaling;
  }

  @Override
  public void prepareRender(
      final MapfishMapContext transformer,
      final MfClientHttpRequestFactory clientHttpRequestFactory) {
    try {
      image = fetchLayerImage(transformer, clientHttpRequestFactory);
    } catch (URISyntaxException | IOException | RuntimeException e) {
      if (failOnError) {
        throw new RuntimeException(e);
      } else {
        LOGGER.error("Error while fetching image", e);
        image = createErrorImage(new Rectangle(1, 1));
        imageLoadError = true;
        imageBufferScaling = 1;
        return;
      }
    }
    imageLoadError = false;

    final ReferencedEnvelope envelopeOrig =
        transformer.getBounds().toReferencedEnvelope(transformer.getPaintArea());
    final Rectangle paintArea = calculateNewBounds(image, envelopeOrig);

    double widthImageBufferScaling = paintArea.getWidth() / transformer.getMapSize().getWidth();
    double heightImageBufferScaling = paintArea.getHeight() / transformer.getMapSize().getHeight();
    imageBufferScaling =
        Math.sqrt(
            (Math.pow(widthImageBufferScaling, 2) + Math.pow(heightImageBufferScaling, 2)) / 2);
  }

  private BufferedImage fetchLayerImage(
      final MapfishMapContext transformer,
      final MfClientHttpRequestFactory clientHttpRequestFactory)
      throws URISyntaxException, IOException {
    BufferedImage image;
    final URI commonUri = new URI(this.params.getBaseUrl());
    final ClientHttpRequest request =
        clientHttpRequestFactory.createRequest(commonUri, HttpMethod.GET);
    return fetchImage(request, transformer);
  }

  /**
   * Renders an image as layer.
   *
   * <p>Type: <code>image</code>
   */
  public static final class ImageLayerPlugin extends AbstractGridCoverageLayerPlugin
      implements MapLayerFactoryPlugin<ImageParam> {
    private static final String TYPE = "image";
    @Autowired private ForkJoinPool forkJoinPool;
    @Autowired private MetricRegistry metricRegistry;

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
    public ImageLayer parse(@Nonnull final Template template, @Nonnull final ImageParam layerData) {
      String styleRef = layerData.style;
      return new ImageLayer(
          this.forkJoinPool,
          super.<GridCoverage2D>createStyleSupplier(template, styleRef),
          layerData,
          template.getConfiguration(),
          metricRegistry);
    }
  }

  /** The parameters for reading an image file, either from the server or from a URL. */
  public static final class ImageParam extends AbstractLayerParams {

    private static final int NUMBER_OF_EXTENT_COORDS = 4;

    /** The base URL for the image file. Used for making request. */
    public String baseURL;

    /** The extent of the image. Used for placing image on map. */
    public double[] extent;

    /** The styles to apply to the image. */
    @HasDefaultValue public String style = Constants.Style.Raster.NAME;

    /** The format of the image. for example image/png, image/jpeg, etc... */
    @HasDefaultValue public String imageFormat = "";

    /** Validate the properties have the correct values. */
    public void postConstruct() {
      Assert.equals(
          NUMBER_OF_EXTENT_COORDS,
          this.extent.length,
          "maxExtent must have exactly 4 elements to the array.  Was: "
              + Arrays.toString(this.extent));
    }

    public String getBaseUrl() {
      return this.baseURL;
    }
  }
}
