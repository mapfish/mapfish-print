/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.readers;

import org.mapfish.print.utils.PJsonArray;

/**
 * Holds the information we need to manage an TSM layer.
 */
public class TmsLayerInfo extends TileCacheLayerInfo {

    public TmsLayerInfo(String resolutions, int width, int height, float minX, float minY, float maxX, float maxY, String format) {
        super(resolutions, width, height, minX, minY, maxX, maxY, format);
    }

    public TmsLayerInfo(PJsonArray resolutions, int width, int height, float minX, float minY, float maxX, float maxY, String extension) {
        super(resolutions, width, height, minX, minY, maxX, maxY, extension);
    }

    @Override
    protected float resolutionTolerance() {
        return 1.9f;
    }
}

