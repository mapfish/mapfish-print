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

import org.geotools.data.FeatureSource;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;

import javax.annotation.Nonnull;

/**
 * Function for creating feature source.
 *
 * @author Jesse on 7/3/2014.
 */
public interface FeatureSourceSupplier {
    /**
     * Load/create feature source.
     *  @param requestFactory the factory to use for making http requests
     * @param mapContext object containing the map information like bounds, map size, dpi, rotation, etc...
     */
    @Nonnull
    FeatureSource load(@Nonnull final MfClientHttpRequestFactory requestFactory,
                       @Nonnull MapfishMapContext mapContext);
}
