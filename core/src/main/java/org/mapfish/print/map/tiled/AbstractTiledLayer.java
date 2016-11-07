package org.mapfish.print.map.tiled;

import com.codahale.metrics.MetricRegistry;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;
import org.mapfish.print.map.geotools.StyleSupplier;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class to support implementing layers that consist of Raster tiles which are combined to compose a single raster
 * to be drawn on the map.
 *
 * @author Jesse on 4/3/14.
 */
public abstract class AbstractTiledLayer extends AbstractGeotoolsLayer {


    private final StyleSupplier<GridCoverage2D> styleSupplier;
    private final ForkJoinPool forkJoinPool;
    private final MetricRegistry registry;
    private final ForkJoinPool requestForkJoinPool;
    private TileCacheInformation tileCacheInformation;

    /**
     * The scale ratio between the tiles resolution and the target resolution.
     */
    protected double imageBufferScaling = 1.0;

    /**
     * Constructor.
     * @param forkJoinPool the thread pool for doing the rendering.
     * @param requestForkJoinPool the thread pool for making tile/image requests.
     * @param styleSupplier strategy for loading the style for this layer
     * @param params the parameters for this layer
     * @param registry the metrics registry
     */
    protected AbstractTiledLayer(final ForkJoinPool forkJoinPool,
                                 final ForkJoinPool requestForkJoinPool,
                                 final StyleSupplier<GridCoverage2D> styleSupplier,
                                 final AbstractLayerParams params,
                                 final MetricRegistry registry) {
        super(forkJoinPool, params);
        this.forkJoinPool = forkJoinPool;
        this.requestForkJoinPool = requestForkJoinPool;
        this.styleSupplier = styleSupplier;
        this.registry = registry;
    }

    @Override
    public final void prepareRender(
            final MapfishMapContext mapContext) {
        double dpi = mapContext.getDPI();
        MapBounds bounds = mapContext.getBounds();
        Rectangle paintArea = new Rectangle(mapContext.getMapSize());
        this.tileCacheInformation = createTileInformation(bounds, paintArea, dpi);
    }

    @Override
    protected final List<? extends Layer> getLayers(final MfClientHttpRequestFactory httpRequestFactory,
                                                    final MapfishMapContext mapContext) throws Exception {
        double dpi = mapContext.getDPI();
        final TileLoaderTask task = new TileLoaderTask(
                httpRequestFactory, dpi, mapContext,
                this.tileCacheInformation, getFailOnError(), this.requestForkJoinPool, this.registry);
        final GridCoverage2D gridCoverage2D = this.forkJoinPool.invoke(task);

        GridCoverageLayer layer = new GridCoverageLayer(gridCoverage2D, this.styleSupplier.load(httpRequestFactory, gridCoverage2D,
                mapContext));
        return Collections.singletonList(layer);
    }

    /**
     * Create the tile cache information object for the given parameters.
     *
     * @param bounds    the map bounds
     * @param paintArea the area to paint
     * @param dpi       the DPI to render at
     */
    protected abstract TileCacheInformation createTileInformation(MapBounds bounds, Rectangle paintArea, double dpi);

    @Override
    public final double getImageBufferScaling() {
        return this.imageBufferScaling;
    }
}
