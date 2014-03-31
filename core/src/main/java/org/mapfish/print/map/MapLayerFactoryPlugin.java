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

package org.mapfish.print.map;

import com.google.common.base.Optional;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonObject;

import javax.annotation.Nonnull;

/**
 * Parses layer request data and creates a MapLayer from it.
 *
 * @author Jesse on 3/26/14.
 */
public interface MapLayerFactoryPlugin {
    /**
     * Inspect the json data and return Optional&lt;MapLayer> or Optional.absent().
     *
     * @param template the configuration related to the current request.
     * @param layerJson the layer data to parse.
     */
    @Nonnull
    Optional<? extends MapLayer> parse(Template template, @Nonnull PJsonObject layerJson) throws Throwable;

}
