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

package org.mapfish.print.map.geotools;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapTransformer;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Jesse on 3/26/14.
 */
public class AbstractGridCoverage2DReaderLayer extends AbstractGeotoolsLayer {

    private final List<? extends Layer> layers;

    /**
     * Constructor.
     *
     * @param coverage2DReader the coverage2DReader for reading the grid coverage data.
     * @param style            style to use for rendering the data.
     * @param executorService  the thread pool for doing the rendering.
     */
    public AbstractGridCoverage2DReaderLayer(final AbstractGridCoverage2DReader coverage2DReader, final Style style,
                                             final ExecutorService executorService) {
        super(executorService);
        this.layers = Collections.singletonList(new GridReaderLayer(coverage2DReader, style));
    }

    @Override
    public final List<? extends Layer> getLayers(final MapBounds bounds, final Rectangle paintArea, final double dpi,
                                                 final MapTransformer transformer, final boolean isFirstLayer) {
        return this.layers;
    }

}
