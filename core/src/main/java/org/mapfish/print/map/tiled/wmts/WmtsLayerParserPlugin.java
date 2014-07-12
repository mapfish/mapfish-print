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

package org.mapfish.print.map.tiled.wmts;

import com.google.common.collect.Sets;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import javax.annotation.Nonnull;

/**
 * The Plugin for creating WMTS layers.
 *
 * @author Jesse on 4/3/14.
 */
public final class WmtsLayerParserPlugin extends AbstractGridCoverageLayerPlugin implements MapLayerFactoryPlugin<WMTSLayerParam> {
    @Autowired
    private ForkJoinPool forkJoinPool;

    private Set<String> typenames = Sets.newHashSet("wmts");

    @Override
    public Set<String> getTypeNames() {
        return this.typenames;
    }

    @Override
    public WMTSLayerParam createParameter() {
        return new WMTSLayerParam();
    }

    @Nonnull
    @Override
    public WMTSLayer parse(@Nonnull final Template template,
                          @Nonnull final WMTSLayerParam param) throws Throwable {
        String styleRef = param.rasterStyle;
        return new WMTSLayer(this.forkJoinPool,
                super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                param);
    }
}
