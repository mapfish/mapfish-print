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
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.Set;
import javax.annotation.Nonnull;

import static org.mapfish.print.Constants.RASTER_STYLE_NAME;

/**
 * The Plugin for creating WMTS layers.
 *
* @author Jesse on 4/3/14.
*/
public final class WmtsLayerParserPlugin implements MapLayerFactoryPlugin<WMTSLayerParam> {
    @Autowired
    private StyleParser parser;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private ClientHttpRequestFactory httpRequestFactory;

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
    public MapLayer parse(@Nonnull final Template template, @Nonnull final WMTSLayerParam param) throws Throwable {

        String styleRef = param.rasterStyle;
        Style style = template.getStyle(styleRef)
                .or(this.parser.loadStyle(template.getConfiguration(), styleRef))
                .or(template.getConfiguration().getDefaultStyle(RASTER_STYLE_NAME));
        return new WMTSLayer(this.forkJoinPool, style, param, this.httpRequestFactory);
    }
}
