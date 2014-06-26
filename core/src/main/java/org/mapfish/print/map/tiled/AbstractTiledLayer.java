/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.tiled;

import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapTransformer;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.springframework.http.client.ClientHttpRequestFactory;

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
    private volatile GridCoverageLayer layer;

    /**
     * Constructor.
     * @param forkJoinPool the thread pool for doing the rendering.
     * @param styleSupplier strategy for loading the style for this layer
     */
    protected AbstractTiledLayer(final ForkJoinPool forkJoinPool, final StyleSupplier<GridCoverage2D> styleSupplier) {
        super(forkJoinPool);
        this.forkJoinPool = forkJoinPool;
        this.styleSupplier = styleSupplier;
    }

    @Override
    protected final List<? extends Layer> getLayers(final ClientHttpRequestFactory httpRequestFactory,
                                                    final MapBounds bounds,
                                                    final Rectangle paintArea,
                                                    final double dpi,
                                                    final MapTransformer transformer,
                                                    final boolean isFirstLayer) {
        if (this.layer == null) {
            synchronized (this) {
                if (this.layer == null) {
                    TileCacheInformation tileCacheInformation = createTileInformation(bounds, paintArea, dpi, isFirstLayer);
                    final TileLoaderTask task = new TileLoaderTask(httpRequestFactory, bounds, paintArea, dpi,
                            transformer, tileCacheInformation);
                    final GridCoverage2D gridCoverage2D = this.forkJoinPool.invoke(task);

                    this.layer = new GridCoverageLayer(gridCoverage2D, this.styleSupplier.load(httpRequestFactory, gridCoverage2D));
                }
            }
        }
        return Collections.singletonList(this.layer);
    }

    /**
     * Create the tile cache information object for the given parameters.
     *
     * @param bounds    the map bounds
     * @param paintArea the area to paint
     * @param dpi       the DPI to render at
     * @param isFirstLayer true indicates this layer is the first layer in the map (the first layer drawn, ie the base layer)
     */
    protected abstract TileCacheInformation createTileInformation(MapBounds bounds, Rectangle paintArea, double dpi,
                                                                  final boolean isFirstLayer);

}
