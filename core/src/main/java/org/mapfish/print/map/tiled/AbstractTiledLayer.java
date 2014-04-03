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
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;

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


    private final Style rasterStyle;
    private final ForkJoinPool forkJoinPool;
    private volatile GridCoverageLayer layer;

    /**
     * Constructor.
     *
     * @param forkJoinPool the thread pool for doing the rendering.
     * @param rasterStyle  the style to use when drawing the constructed grid coverage on the map.
     */
    protected AbstractTiledLayer(final ForkJoinPool forkJoinPool, final Style rasterStyle) {
        super(forkJoinPool);
        this.forkJoinPool = forkJoinPool;
        this.rasterStyle = rasterStyle;
    }

    @Override
    protected final List<? extends Layer> getLayers(final MapBounds bounds, final Rectangle paintArea, final double dpi,
                                                    final boolean isFirstLayer) {
        if (this.layer == null) {
            synchronized (this) {
                if (this.layer == null) {
                    TileCacheInformation tileCacheInformation = createTileInformation(bounds, paintArea, dpi, isFirstLayer);
                    final GridCoverage2D gridCoverage2D = this.forkJoinPool.invoke(new TileLoaderTask(bounds, paintArea, dpi,
                            tileCacheInformation));
                    this.layer = new GridCoverageLayer(gridCoverage2D, this.rasterStyle);
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
