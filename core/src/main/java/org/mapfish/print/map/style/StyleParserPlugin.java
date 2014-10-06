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
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A plugin used for loading {@link org.geotools.styling.Style} objects from a string.
 *
 * The string might be json, css, url, whatever.
 *
* @author Jesse on 3/26/14.
*/
public interface StyleParserPlugin {

    /**
     * Using the string load a style.  The string can be from a URL, xml, css, whatever.  If the string
     * references a file it <strong>MUST</strong> be within a subdirectory of the configuration directory.
     *
     * @param configuration the configuration being used for the current print.
     * @param clientHttpRequestFactory an factory for making http requests.
     * @param styleString the string that provides the information for loading the style.
     * @param mapContext information about the map projection, bounds, size, etc...
     *
     * @return if this plugin can create a style form the string then return the style otherwise Optional.absent().
     */
    Optional<Style> parseStyle(@Nullable Configuration configuration,
                               @Nonnull ClientHttpRequestFactory clientHttpRequestFactory,
                               @Nullable String styleString,
                               @Nonnull MapfishMapContext mapContext) throws Throwable;
}
