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
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import org.geotools.data.FeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapBounds;

import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * A layer that wraps a Geotools Feature Source and a style object.
 *
 * @author Jesse on 3/26/14.
 */
public abstract class AbstractFeatureSourceLayer extends AbstractGeotoolsLayer {

    private final Supplier<FeatureSource> featureSourceSupplier;
    private final Function<FeatureSource, Style> styleSupplier;
    private volatile List<? extends Layer> layers;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be called once.
     * @param styleSupplier         a function that creates the style for styling the features. This will only be called once.
     */
    public AbstractFeatureSourceLayer(final ExecutorService executorService, final Supplier<FeatureSource> featureSourceSupplier,
                                      final Function<FeatureSource, Style> styleSupplier) {
        super(executorService);
        this.featureSourceSupplier = featureSourceSupplier;
        this.styleSupplier = styleSupplier;
    }

    @Override
    public final List<? extends Layer> getLayers(final MapBounds bounds, final Rectangle paintArea, final double dpi,
                                                 final boolean isFirstLayer) {
        if (this.layers == null) {
                synchronized (this) {
                    if (this.layers == null) {
                        FeatureSource source = this.featureSourceSupplier.get();
                        Style style = this.styleSupplier.apply(source);
                        this.layers = Lists.newArrayList(new FeatureLayer(source, style));
                    }
                }
            }
            return this.layers;
        }
}
