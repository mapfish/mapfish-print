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

package org.mapfish.print.map.geotools.grid;

import org.geotools.data.FeatureSource;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.mapfish.print.map.geotools.StyleSupplier;

import java.util.concurrent.ExecutorService;

/**
 * A layer which is a spatial grid of lines on the map.
 *
 * @author Jesse on 7/2/2014.
 */
public class GridLayer extends AbstractFeatureSourceLayer {
    /**
     * Constructor.
     *
     * @param executorService       the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be called once.
     * @param styleSupplier         a function that creates the style for styling the features. This will only be called once.
     * @param renderAsSvg           is the layer rendered as SVG?
     * @param params                the parameters for this layer
     */
    public GridLayer(final ExecutorService executorService,
                     final FeatureSourceSupplier featureSourceSupplier,
                     final StyleSupplier<FeatureSource> styleSupplier,
                     final boolean renderAsSvg,
                     final AbstractLayerParams params) {
        super(executorService, featureSourceSupplier, styleSupplier, renderAsSvg, params);
    }

}
