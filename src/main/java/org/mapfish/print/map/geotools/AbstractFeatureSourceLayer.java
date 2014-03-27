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

import org.geotools.data.FeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Jesse on 3/26/14.
 */
public class AbstractFeatureSourceLayer extends AbstractGeotoolsLayer {

    private final List<FeatureLayer> layers;

    /**
     * Constructor.
     *
     * @param featureSource the featureSource containing the feature data.
     * @param style style to use for rendering the data.
     * @param executorService the thread pool for doing the rendering.
     */
    public AbstractFeatureSourceLayer(final FeatureSource featureSource, final Style style, final ExecutorService executorService) {
        super(executorService);
        this.layers = Collections.singletonList(new FeatureLayer(featureSource, style));
    }

    @Override
    protected final List<? extends Layer> getLayers() {
        return this.layers;
    }
}
