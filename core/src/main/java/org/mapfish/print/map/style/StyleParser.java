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

package org.mapfish.print.map.style;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Parse a style using all the available {@link StyleParserPlugin} registered with the spring application context.
 *
 * @author Jesse on 3/26/14.
 */
public final class StyleParser {
    @Autowired
    private List<StyleParserPlugin> plugins = Lists.newArrayList();

    /**
     * Load style using one of the plugins or return Optional.absent().
     *  @param configuration the configuration for the current request.
     * @param clientHttpRequestFactory a factory for making http requests
     * @param styleString   the style to load.
     * @param mapContext information about the map projection, bounds, size, etc...
     */
    public Optional<? extends Style> loadStyle(final Configuration configuration,
                                               @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
                                               final String styleString,
                                               final MapfishMapContext mapContext) {
        for (StyleParserPlugin plugin : this.plugins) {
            try {
                Optional<? extends Style> style = plugin.parseStyle(configuration, clientHttpRequestFactory, styleString, mapContext);
                if (style.isPresent()) {
                    return style;
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return Optional.absent();
    }
}
