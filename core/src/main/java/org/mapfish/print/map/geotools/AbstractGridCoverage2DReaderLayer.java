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

import com.google.common.base.Function;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Jesse on 3/26/14.
 */
public class AbstractGridCoverage2DReaderLayer extends AbstractGeotoolsLayer {

    private final Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> coverage2DReaderSupplier;
    private final StyleSupplier<AbstractGridCoverage2DReader> styleSupplier;

    /**
     * Constructor.
     *
     * @param coverage2DReader the coverage2DReader for reading the grid coverage data.
     * @param style            style to use for rendering the data.
     * @param executorService  the thread pool for doing the rendering.
     */
    public AbstractGridCoverage2DReaderLayer(final Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> coverage2DReader,
                                             final StyleSupplier<AbstractGridCoverage2DReader> style,
                                             final ExecutorService executorService) {
        super(executorService);
        this.styleSupplier = style;
        this.coverage2DReaderSupplier = coverage2DReader;
    }

    @Override
    public final synchronized List<? extends Layer> getLayers(final MfClientHttpRequestFactory httpRequestFactory,
                                                 final MapfishMapContext mapContext,
                                                 final boolean isFirstLayer) throws Exception {
        AbstractGridCoverage2DReader coverage2DReader = this.coverage2DReaderSupplier.apply(httpRequestFactory);
        Style style = this.styleSupplier.load(httpRequestFactory, coverage2DReader, mapContext);
        return Collections.singletonList(new GridReaderLayer(coverage2DReader, style));
    }

}
