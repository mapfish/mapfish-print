package org.mapfish.print.map.image;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.geotools.api.style.Style;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.mapfish.print.PrintException;
import org.mapfish.print.StatsUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.http.Utils;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/** Common implementation for layers that are represented as a single grid coverage image. */
public abstract class AbstractSingleImageLayer extends AbstractGeotoolsLayer {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSingleImageLayer.class);

  /** The metrics object. */
  @Nonnull protected final MetricRegistry registry;

  /** The configuration. */
  protected final Configuration configuration;

  private final StyleSupplier<GridCoverage2D> styleSupplier;

  /**
   * Constructor.
   *
   * @param executorService the thread pool for doing the rendering.
   * @param styleSupplier the style to use when drawing the constructed grid coverage on the map.
   * @param params the parameters for this layer
   * @param registry the metrics object.
   * @param configuration the configuration
   */
  protected AbstractSingleImageLayer(
      final ExecutorService executorService,
      final StyleSupplier<GridCoverage2D> styleSupplier,
      final AbstractLayerParams params,
      @Nonnull final MetricRegistry registry,
      final Configuration configuration) {
    super(executorService, params);
    this.styleSupplier = styleSupplier;
    this.registry = registry;
    this.configuration = configuration;
  }

  @Override
  protected final List<? extends Layer> getLayers(
      final MfClientHttpRequestFactory httpRequestFactory,
      final MapfishMapContext mapContext,
      final Processor.ExecutionContext context,
      final LayerContext layerContext) {
    BufferedImage image;
    try {
      image = loadImage(httpRequestFactory, mapContext);
    } catch (RuntimeException t) {
      throw t;
    } catch (Exception t) {
      throw new PrintException("Failed to LoadImage", t);
    }

    final MapBounds bounds = mapContext.getBounds();
    final ReferencedEnvelope mapEnvelope = bounds.toReferencedEnvelope(mapContext.getPaintArea());

    GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
    GeneralBounds gridEnvelope = new GeneralBounds(mapEnvelope.getCoordinateReferenceSystem());
    gridEnvelope.setEnvelope(
        mapEnvelope.getMinX(), mapEnvelope.getMinY(),
        mapEnvelope.getMaxX(), mapEnvelope.getMaxY());
    final String coverageName = getClass().getSimpleName();
    final GridCoverage2D gridCoverage2D =
        factory.create(coverageName, image, gridEnvelope, null, null, null);

    Style style = this.styleSupplier.load(httpRequestFactory, gridCoverage2D);
    return Collections.singletonList(new GridCoverageLayer(gridCoverage2D, style));
  }

  /**
   * Load the image at the requested size for the provided map bounds.
   *
   * @param requestFactory the factory to use for making http requests
   * @param transformer object containing map rendering information
   */
  protected abstract BufferedImage loadImage(
      MfClientHttpRequestFactory requestFactory, MapfishMapContext transformer) throws Exception;

  public StyleSupplier<GridCoverage2D> getStyleSupplier() {
    return styleSupplier;
  }

  /**
   * Create an error image.
   *
   * @param area The size of the image
   */
  protected BufferedImage createErrorImage(final Rectangle area) {
    final BufferedImage bufferedImage =
        new BufferedImage(area.width, area.height, TYPE_INT_ARGB_PRE);
    final Graphics2D graphics = bufferedImage.createGraphics();
    try {
      graphics.setBackground(
          ColorParser.toColor(this.configuration.getTransparentTileErrorColor()));
      graphics.clearRect(0, 0, area.width, area.height);
      return bufferedImage;
    } finally {
      graphics.dispose();
    }
  }

  /**
   * Fetch the given image from the web.
   *
   * @param request The request
   * @param transformer The transformer
   * @return The image
   */
  protected BufferedImage fetchImage(
      @Nonnull final ClientHttpRequest request, @Nonnull final MapfishMapContext transformer)
      throws IOException {
    final String baseMetricName = getBaseMetricName(request);
    try (Timer.Context ignored = this.registry.timer(baseMetricName).time()) {
      try (ClientHttpResponse httpResponse = request.execute()) {
        final List<String> contentType = httpResponse.getHeaders().get("Content-Type");
        final String invalidRespBody = getInvalidResponseBody(request, contentType, httpResponse);

        if (!isResponseStatusCodeValid(request, httpResponse, invalidRespBody, baseMetricName)) {
          return createErrorImage(transformer.getPaintArea());
        }

        if (!isResponseBodyValid(invalidRespBody, request, contentType, baseMetricName)) {
          return createErrorImage(transformer.getPaintArea());
        }

        return fetchImageFromHttpResponse(request, httpResponse, transformer, baseMetricName);
      } catch (RuntimeException e) {
        this.registry.counter(MetricRegistry.name(baseMetricName, "error")).inc();
        throw e;
      }
    }
  }

  private String getBaseMetricName(@Nonnull final ClientHttpRequest request) {
    return MetricRegistry.name(
        getClass().getSimpleName(), "read", StatsUtils.quotePart(request.getURI().getHost()));
  }

  private static String getInvalidResponseBody(
      final ClientHttpRequest request,
      final List<String> contentType,
      final ClientHttpResponse httpResponse)
      throws IOException {
    if (contentType == null || contentType.size() != 1) {
      LOGGER.debug("The image {} didn't return a valid content type header.", request.getURI());
    } else if (!contentType.getFirst().startsWith("image/")) {
      final byte[] data;
      try (InputStream body = httpResponse.getBody()) {
        data = IOUtils.toByteArray(body);
      }
      return new String(data, StandardCharsets.UTF_8);
    }
    return null;
  }

  private boolean isResponseStatusCodeValid(
      final ClientHttpRequest request,
      final ClientHttpResponse httpResponse,
      final String stringBody,
      final String baseMetricName)
      throws IOException {
    if (httpResponse.getStatusCode().value() != HttpStatus.OK.value()) {
      String message =
          String.format(
              """
              Invalid status code for %s (%d!=%d), status: %s. With request headers:
              %s
              The response was: '%s'
              With response headers:
              %s\
              """,
              request.getURI(),
              httpResponse.getStatusCode().value(),
              HttpStatus.OK.value(),
              httpResponse.getStatusText(),
              String.join("\n", Utils.getPrintableHeadersList(request.getHeaders())),
              httpResponse.getStatusText(),
              String.join("\n", Utils.getPrintableHeadersList(httpResponse.getHeaders())));
      if (stringBody != null) {
        message += "\nContent:\n" + stringBody;
      }
      this.registry.counter(MetricRegistry.name(baseMetricName, "error")).inc();
      if (getFailOnError()) {
        throw new RuntimeException(message);
      } else {
        LOGGER.warn(message);
        return false;
      }
    }
    return true;
  }

  private boolean isResponseBodyValid(
      final String responseBody,
      final ClientHttpRequest request,
      final List<String> contentType,
      final String baseMetricName) {
    if (responseBody != null) {
      LOGGER.debug(
          "We get a wrong image for {}, content type: {}\nresult:\n{}",
          request.getURI(),
          contentType.getFirst(),
          responseBody);
      this.registry.counter(MetricRegistry.name(baseMetricName, "error")).inc();
      if (getFailOnError()) {
        throw new RuntimeException("Wrong content-type : " + contentType.getFirst());
      } else {
        return false;
      }
    }
    return true;
  }

  private BufferedImage fetchImageFromHttpResponse(
      final ClientHttpRequest request,
      final ClientHttpResponse httpResponse,
      final MapfishMapContext transformer,
      final String baseMetricName)
      throws IOException {
    final BufferedImage image = ImageIO.read(httpResponse.getBody());
    if (image == null) {
      LOGGER.warn("Cannot read image from {}", request.getURI());
      this.registry.counter(MetricRegistry.name(baseMetricName, "error")).inc();
      if (getFailOnError()) {
        throw new RuntimeException("Cannot read image from " + request.getURI());
      }
      return createErrorImage(transformer.getPaintArea());
    }
    return image;
  }
}
