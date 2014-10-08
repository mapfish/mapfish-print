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
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleParser.class);
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
        if (styleString != null) {
        for (StyleParserPlugin plugin : this.plugins) {
            try {
                Optional<? extends Style> style = plugin.parseStyle(configuration, clientHttpRequestFactory, styleString, mapContext);
                if (style.isPresent()) {
                    if (LOGGER.isDebugEnabled()) {
                        try {
                            final SLDTransformer transformer = new SLDTransformer();
                            final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
                            final UserLayer userLayer = styleFactory.createUserLayer();
                            userLayer.addUserStyle(style.get());
                            final StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();
                            sld.addStyledLayer(userLayer);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Loaded style from: \n\n '" + styleString + "': \n\n" + transformer.transform(sld));
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Loaded style from: \n\n '" + styleString + "' \n\n<Unable to transform it to xml>: " + e, e);
                        }
                    }
                    return style;
                }
            } catch (Throwable t) {
                throw ExceptionUtils.getRuntimeException(t);
            }
        }
        }
        return Optional.absent();
    }
}
