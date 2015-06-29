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

import jsr166y.ForkJoinPool;
import org.geotools.data.FeatureSource;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayerPlugin;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;

/**
 * The plugin for creating the grid layer.
 */
public final class GridLayerPlugin extends AbstractFeatureSourceLayerPlugin<GridParam> {

    private static final String TYPE = "grid";
    @Autowired
    private ForkJoinPool pool;

    /**
     * Constructor.
     */
    public GridLayerPlugin() {
        super(TYPE);
    }

    @Override
    public GridParam createParameter() {
        return new GridParam();
    }

    @Nonnull
    @Override
    public GridLayer parse(@Nonnull final Template template, @Nonnull final GridParam layerData) throws Throwable {

        FeatureSourceSupplier featureSource = createFeatureSourceFunction(template, layerData);
        final StyleSupplier<FeatureSource> styleFunction = createStyleSupplier(template, layerData);
        return new GridLayer(this.pool, featureSource, styleFunction,
                template.getConfiguration().renderAsSvg(layerData.renderAsSvg),
                layerData);
    }

    private StyleSupplier<FeatureSource> createStyleSupplier(final Template template, final GridParam layerData) {

        return new StyleSupplier<FeatureSource>() {
            @Override
            public Style load(final MfClientHttpRequestFactory requestFactory,
                              final FeatureSource featureSource,
                              final MapfishMapContext mapContext) {
                String styleRef = layerData.style;
                return template.getStyle(styleRef, mapContext)
                        .or(GridLayerPlugin.super.parser.loadStyle(
                                template.getConfiguration(),
                                requestFactory, styleRef, mapContext))
                        .or(layerData.gridType.strategy.defaultStyle(template, layerData));
            }
        };
    }

    private FeatureSourceSupplier createFeatureSourceFunction(final Template template,
                                                              final GridParam layerData) {

        return layerData.gridType.strategy.createFeatureSource(template, layerData);
    }

}
