package org.mapfish.print.map.tiled;

import com.codahale.metrics.MetricRegistry;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.HttpRequestCache;
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
 */
public abstract class AbstractTiledLayer extends AbstractGeotoolsLayer {


    private final StyleSupplier<GridCoverage2D> styleSupplier;
    private final MetricRegistry registry;
    private TileCacheInformation tileCacheInformation;
    private TilePreparationInfo tilePreparationInfo;

    /**
     * The scale ratio between the tiles resolution and the target resolution.
     */
    protected double imageBufferScaling = 1.0;

    /**
     * Constructor.
     * @param forkJoinPool the thread pool for doing the rendering.
     * @param styleSupplier strategy for loading the style for this layer
     * @param params the parameters for this layer
     * @param registry the metrics registry
     */
    protected AbstractTiledLayer(final ForkJoinPool forkJoinPool,
                                 final StyleSupplier<GridCoverage2D> styleSupplier,
                                 final AbstractLayerParams params,
                                 final MetricRegistry registry) {
        super(forkJoinPool, params);
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
        
        final CoverageTask task = new CoverageTask(this.tilePreparationInfo, 
                getFailOnError(), this.registry, this.tileCacheInformation);
        final GridCoverage2D gridCoverage2D = task.call();

        GridCoverageLayer layer = new GridCoverageLayer(
                gridCoverage2D, this.styleSupplier.load(httpRequestFactory, gridCoverage2D));
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
    
    @Override
    public final void cacheResources(final HttpRequestCache httpRequestCache,
            final MfClientHttpRequestFactory clientHttpRequestFactory, final MapfishMapContext transformer) {
        final MapfishMapContext layerTransformer = getLayerTransformer(transformer);
        
        final double dpi = transformer.getDPI();
        final TilePreparationTask task = new TilePreparationTask(
                clientHttpRequestFactory, dpi, layerTransformer,
                this.tileCacheInformation, httpRequestCache);
        this.tilePreparationInfo = task.call();
    }
}
