package org.mapfish.print.map.tiled;

import com.codahale.metrics.MetricRegistry;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.HttpRequestFetcher;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.processor.Processor;

/**
 * An abstract class to support implementing layers that consist of Raster tiles which are combined
 * to compose a single raster to be drawn on the map.
 */
public abstract class AbstractTiledLayer extends AbstractGeotoolsLayer {

  private final StyleSupplier<GridCoverage2D> styleSupplier;
  private final MetricRegistry registry;
  private final Configuration configuration;
  /** The scale ratio between the tiles resolution and the target resolution. */
  protected double imageBufferScaling = 1.0;

  private TileCacheInformation tileCacheInformation;
  private TilePreparationInfo tilePreparationInfo;

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
      @Nonnull final AbstractLayerParams params,
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

  @Override
  public final void prepareRender(final MapfishMapContext mapContext) {
    this.tileCacheInformation =
        createTileInformation(
            mapContext.getRotatedBoundsAdjustedForPreciseRotatedMapSize(),
            new Rectangle(mapContext.getRotatedMapSize()),
            mapContext.getDPI());
  }

  @Override
  protected final List<? extends Layer> getLayers(
      final MfClientHttpRequestFactory httpRequestFactory,
      final MapfishMapContext mapContext,
      final Processor.ExecutionContext context) {

    final CoverageTask task =
        new CoverageTask(
            this.tilePreparationInfo,
            getFailOnError(),
            this.registry,
            context,
            this.tileCacheInformation,
            this.configuration);
    final GridCoverage2D gridCoverage2D = task.call();

    GridCoverageLayer layer =
        new GridCoverageLayer(
            gridCoverage2D, this.styleSupplier.load(httpRequestFactory, gridCoverage2D));
    return Collections.singletonList(layer);
  }

  /**
   * Create the tile cache information object for the given parameters.
   *
   * @param bounds the map bounds
   * @param paintArea the area to paint
   * @param dpi the DPI to render at
   */
  protected abstract TileCacheInformation createTileInformation(
      MapBounds bounds, Rectangle paintArea, double dpi);

  @Override
  public final double getImageBufferScaling() {
    return this.imageBufferScaling;
  }

  @Override
  public final void prefetchResources(
      final HttpRequestFetcher httpRequestFetcher,
      final MfClientHttpRequestFactory clientHttpRequestFactory,
      final MapfishMapContext transformer,
      final Processor.ExecutionContext context) {
    final MapfishMapContext layerTransformer = getLayerTransformer(transformer);

    final TilePreparationTask task =
        new TilePreparationTask(
            clientHttpRequestFactory,
            layerTransformer,
            this.tileCacheInformation,
            httpRequestFetcher,
            context);
    this.tilePreparationInfo = task.call();
  }
}
