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

import com.google.common.collect.Lists;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MapfishClientHttpRequestFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

/**
 * A layer that wraps a Geotools Feature Source and a style object.
 *
 * @author Jesse on 3/26/14.
 */
public abstract class AbstractFeatureSourceLayer extends AbstractGeotoolsLayer {

    private FeatureSourceSupplier featureSourceSupplier;
    private StyleSupplier<FeatureSource> styleSupplier;
    private final Boolean renderAsSvg;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be called once.
     * @param styleSupplier         a function that creates the style for styling the features. This will only be called once.
     * @param renderAsSvg is the layer rendered as SVG?
     */
    public AbstractFeatureSourceLayer(final ExecutorService executorService,
                                      final FeatureSourceSupplier featureSourceSupplier,
                                      final StyleSupplier<FeatureSource> styleSupplier,
                                      final boolean renderAsSvg) {
        super(executorService);
        this.featureSourceSupplier = featureSourceSupplier;
        this.styleSupplier = styleSupplier;

        this.renderAsSvg = renderAsSvg;
    }

    public final void setStyle(final StyleSupplier style) {
        this.styleSupplier = style;
    }

    @Override
    public final List<? extends Layer> getLayers(final MapfishClientHttpRequestFactory httpRequestFactory,
                                                 final MapfishMapContext mapContext,
                                                 final boolean isFirstLayer) throws Exception {
        FeatureSource source = this.featureSourceSupplier.load(httpRequestFactory, mapContext);
        Style style = this.styleSupplier.load(httpRequestFactory, source, mapContext);
        return Lists.newArrayList(new FeatureLayer(source, style));
    }

    public final void setFeatureCollection(final SimpleFeatureCollection featureCollection) {
        this.featureSourceSupplier = new FeatureSourceSupplier() {

            @Nonnull
            @Override
            public FeatureSource load(@Nonnull final MapfishClientHttpRequestFactory requestFactory,
                                      @Nonnull final MapfishMapContext mapContext) {
                return new CollectionFeatureSource(featureCollection);
            }
        };
    }
    
    /**
     * Is the layer rendered as SVG?
     */
    public final Boolean shouldRenderAsSvg() {
        return this.renderAsSvg;
    }
}
