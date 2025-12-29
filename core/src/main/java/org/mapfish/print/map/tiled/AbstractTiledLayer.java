package org.mapfish.print.map.tiled;

import com.codahale.metrics.MetricRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.HttpRequestFetcher;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.processor.Processor;

/**
 * An abstract class to support implementing layers that consist of Raster tiles which are combined
 * to compose a single raster to be drawn on the map.
 *
 * @param <T> Type of the params supported by this layer.
 */
public abstract class AbstractTiledLayer<T extends AbstractTiledLayerParams>
    extends AbstractGeotoolsLayer {

  private final StyleSupplier<GridCoverage2D> styleSupplier;
  private final MetricRegistry registry;
  private final Configuration configuration;

  /**
   * Constructor.
   *
   * @param forkJoinPool the thread pool for doing the rendering.
   * @param styleSupplier strategy for loading the style for this layer.
   * @param params the parameters for this layer.
   * @param registry the metrics registry.
   * @param configuration the configuration.
   */
  protected AbstractTiledLayer(
      @Nullable final ForkJoinPool forkJoinPool,
      @Nullable final StyleSupplier<GridCoverage2D> styleSupplier,
      @Nonnull final T params,
      @Nullable final MetricRegistry registry,
      @Nonnull final Configuration configuration) {
    super(forkJoinPool, params);
    this.styleSupplier = styleSupplier;
    this.registry = registry;
    this.configuration = configuration;
  }

  /**
   * Copy constructor.
   *
   * @param other The source.
   * @param styleSupplier strategy for loading the style for this layer.
   * @param registry the metrics registry.
   * @param configuration the configuration.
   */
  protected AbstractTiledLayer(
      final AbstractGeotoolsLayer other,
      @Nullable final StyleSupplier<GridCoverage2D> styleSupplier,
      @Nullable final MetricRegistry registry,
      @Nonnull final Configuration configuration) {
    super(other);
    this.styleSupplier = styleSupplier;
    this.registry = registry;
    this.configuration = configuration;
  }

  /**
   * Create the tile information and return its ImageBufferingScaling.
   *
   * @param mapContext the map transformer containing the map bounds and size.
   * @param clientHttpRequestFactory the factory to use for making http requests.
   * @return the LayerContext for this requested rendering.
   */
  @Override
  public final LayerContext prepareRender(
      final MapfishMapContext mapContext,
      final MfClientHttpRequestFactory clientHttpRequestFactory) {
    TileInformation<T> tileInformation =
        createTileInformation(
            mapContext.getRotatedBoundsAdjustedForPreciseRotatedMapSize(),
            new Rectangle(mapContext.getRotatedMapSize()),
            mapContext.getDPI());

    return new LayerContext(tileInformation.getImageBufferScaling(), tileInformation, null);
  }

  @Override
  protected final List<? extends Layer> getLayers(
      final MfClientHttpRequestFactory httpRequestFactory,
      final MapfishMapContext mapContext,
      final Processor.ExecutionContext context,
      final LayerContext layerContext) {

    final CoverageTask task =
        new CoverageTask(
            layerContext.tilePreparationInfo(),
            getFailOnError(),
            this.registry,
            context,
            layerContext.tileInformation(),
            this.configuration);
    final GridCoverage2D gridCoverage2D = task.call();

    GridCoverageLayer layer =
        new GridCoverageLayer(
            gridCoverage2D, this.styleSupplier.load(httpRequestFactory, gridCoverage2D));
    return Collections.singletonList(layer);
  }

  /**
   * Create the tile information object for the given parameters.
   *
   * @param bounds the map bounds
   * @param paintArea the area to paint
   * @param dpi the DPI to render at
   */
  protected abstract TileInformation<T> createTileInformation(
      MapBounds bounds, Rectangle paintArea, double dpi);

  @Override
  public final LayerContext prefetchResources(
      final HttpRequestFetcher httpRequestFetcher,
      final MfClientHttpRequestFactory clientHttpRequestFactory,
      final MapfishMapContext transformer,
      final Processor.ExecutionContext context,
      final LayerContext layerContext) {
    final MapfishMapContext layerTransformer = getLayerTransformer(transformer);

    final TilePreparationTask task =
        new TilePreparationTask(
            clientHttpRequestFactory,
            layerTransformer,
            layerContext.tileInformation(),
            httpRequestFetcher,
            context);
    TilePreparationInfo tilePreparationInfo = task.call();
    return new LayerContext(
        layerContext.scale(), layerContext.tileInformation(), tilePreparationInfo);
  }
}
