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

import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;

/**
 * A strategy for loading style objects.
 *
 * @author Jesse on 6/25/2014.
 *
 * @param <Source> the type source that the style applies to
 */
public interface StyleSupplier<Source> {
    /**
     * Load the style.
     * @param requestFactory the factory to use for making http requests
     * @param featureSource the source the style applies to
     * @param mapContext information about the map projection, bounds, size, etc...
     */
    Style load(final MfClientHttpRequestFactory requestFactory,
               final Source featureSource,
               final MapfishMapContext mapContext) throws Exception;
}
