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

package org.mapfish.print.map.image.wms;

import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Layer plugin factory for creating WMS layers.
 *
 * @author Jesse on 4/10/2014.
 */
public final class WmsLayerFactoryPlugin extends AbstractGridCoverageLayerPlugin implements MapLayerFactoryPlugin<WmsLayerParam> {
    private static final String TYPE = "wms";
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private StyleParser styleParser;


    @Override
    public Set<String> getTypeNames() {
        return Collections.singleton(TYPE);
    }

    @Override
    public WmsLayerParam createParameter() {
        return new WmsLayerParam();
    }

    @Nonnull
    @Override
    public WmsLayer parse(@Nonnull final Template template,
                          @Nonnull final WmsLayerParam layerData) throws Throwable {

        String styleRef = layerData.rasterStyle;
        return new WmsLayer(this.forkJoinPool,
                super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                layerData);
    }
}
