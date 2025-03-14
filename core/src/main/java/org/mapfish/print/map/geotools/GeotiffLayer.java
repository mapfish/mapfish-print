package org.mapfish.print.map.geotools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.mapfish.print.Constants;
import org.mapfish.print.FileUtils;
import org.mapfish.print.PrintException;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.processor.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/** Reads a GeoTIFF file from a URL. */
public final class GeotiffLayer extends AbstractGeotoolsLayer {
  private final Function<MfClientHttpRequestFactory, @Nullable AbstractGridCoverage2DReader>
      coverage2DReaderSupplier;
  private final StyleSupplier<AbstractGridCoverage2DReader> styleSupplier;

  /**
   * Constructor.
   *
   * @param reader the reader to use for reading the geotiff.
   * @param style style to use for rendering the data.
   * @param executorService the thread pool for doing the rendering.
   * @param params the parameters for this layer
   */
  public GeotiffLayer(
      final Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> reader,
      final StyleSupplier<AbstractGridCoverage2DReader> style,
      final ExecutorService executorService,
      final AbstractLayerParams params) {
    super(executorService, params);
    this.styleSupplier = style;
    this.coverage2DReaderSupplier = reader;
  }

  @Override
  public RenderType getRenderType() {
    return RenderType.TIFF;
  }

  @Override
  public LayerContext prepareRender(
      final MapfishMapContext transformer,
      final MfClientHttpRequestFactory clientHttpRequestFactory) {
    return new LayerContext(null, DEFAULT_SCALING, null);
  }

  @Override
  public synchronized List<? extends Layer> getLayers(
      final MfClientHttpRequestFactory httpRequestFactory,
      final MapfishMapContext mapContext,
      final Processor.ExecutionContext context,
      final LayerContext layerContext) {
    AbstractGridCoverage2DReader coverage2DReader =
        this.coverage2DReaderSupplier.apply(httpRequestFactory);
    Style style = this.styleSupplier.load(httpRequestFactory, coverage2DReader);
    return Collections.singletonList(new GridReaderLayer(coverage2DReader, style));
  }

  /**
   * Renders a GeoTIFF image as layer.
   *
   * <p>Type: <code>geotiff</code>
   */
  public static final class Plugin extends AbstractGridCoverageLayerPlugin
      implements MapLayerFactoryPlugin<GeotiffParam> {
    private static final Set<String> TYPENAMES = Collections.singleton("geotiff");
    @Autowired private ExecutorService forkJoinPool;

    @Override
    public Set<String> getTypeNames() {
      return TYPENAMES;
    }

    @Override
    public GeotiffParam createParameter() {
      return new GeotiffParam();
    }

    @Nonnull
    @Override
    public GeotiffLayer parse(@Nonnull final Template template, @Nonnull final GeotiffParam param)
        throws IOException {
      Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> geotiffReader =
          getGeotiffReader(template, param.url);

      String styleRef = param.style;

      return new GeotiffLayer(
          geotiffReader, super.createStyleSupplier(template, styleRef), this.forkJoinPool, param);
    }

    private Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> getGeotiffReader(
        final Template template, final String geotiffUrl) throws IOException {
      final URL url =
          FileUtils.testForLegalFileUrl(template.getConfiguration(), new URL(geotiffUrl));
      return (final MfClientHttpRequestFactory requestFactory) -> {
        try {
          final File geotiffFile;
          if (url.getProtocol().equalsIgnoreCase("file")) {
            geotiffFile = new File(url.toURI());
          } else {
            geotiffFile = File.createTempFile("downloadedGeotiff", ".tiff");

            final ClientHttpRequest request =
                requestFactory.createRequest(url.toURI(), HttpMethod.GET);
            try (ClientHttpResponse httpResponse = request.execute();
                FileOutputStream output = new FileOutputStream(geotiffFile)) {
              IOUtils.copy(httpResponse.getBody(), output);
            }
          }

          return new GeoTiffFormat().getReader(geotiffFile);
        } catch (IOException | URISyntaxException e) {
          throw new PrintException("Failed to get GeotiffReader", e);
        }
      };
    }
  }

  /** The parameters for reading a Geotiff file, either from the server or from a URL. */
  public static final class GeotiffParam extends AbstractLayerParams {
    /**
     * The url of the geotiff. It can be a file but if it is the file must be contained within the
     * config directory.
     */
    public String url;

    /** A string identifying a style to use when rendering the raster. */
    @HasDefaultValue public String style = Constants.Style.Raster.NAME;
  }
}
