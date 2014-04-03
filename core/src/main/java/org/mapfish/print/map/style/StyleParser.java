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
import org.geotools.styling.Style;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse a style using all the available {@link StyleParserPlugin} registered with the spring application context.
 *
 * @author Jesse on 3/26/14.
 */
public final class StyleParser {
    @Autowired
    private List<StyleParserPlugin> plugins;

    /**
     * Load a map of named styles.
     *
     * @param config the current configuration
     * @param parser the parser to do the parsing
     * @param styles the name->sldIdentifier map
     */
    public static Map<String, Style> loadStyles(final Configuration config, final StyleParser parser,
                                                final Map<String, String> styles) {
        Map<String, Style> map = new HashMap<String, Style>(styles.size());

        for (Map.Entry<String, String> entry : styles.entrySet()) {
            Optional<? extends Style> style = parser.loadStyle(config, entry.getValue());
            if (style.isPresent()) {
                map.put(entry.getKey(), style.get());
            } else {
                throw new RuntimeException("Unable to load style: " + entry);
            }
        }
        return map;
    }

    /**
     * Load style using one of the plugins or return Optional.absent().
     *
     * @param configuration the configuration for the current request.
     * @param styleString   the style to load.
     */
    public Optional<? extends Style> loadStyle(final Configuration configuration, final String styleString) {
        for (StyleParserPlugin plugin : this.plugins) {
            try {
                Optional<? extends Style> style = plugin.parseStyle(configuration, styleString);
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
