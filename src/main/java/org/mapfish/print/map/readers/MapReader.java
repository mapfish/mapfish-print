/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.map.readers;

import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.utils.PJsonObject;

/**
 * Logic to read and render a map layer.
 */
public abstract class MapReader {
    protected final float opacity;

    public MapReader(PJsonObject params) {
        opacity = params.optFloat("opacity", 1.0F);
    }

    /**
     * Method called to render a whole layer
     */
    public abstract void render(Transformer transformer, ParallelMapTileLoader parallelMapTileLoader, String srs, boolean first);

    public abstract boolean testMerge(MapReader other);

    /**
     * Test if two layers can be merged (this and other). If it's the case,
     * merge other inside this and return true.
     *
     * @return False if no merge occured.
     */
    protected boolean canMerge(MapReader other) {
        return opacity == other.opacity;
    }

    public abstract String toString();

}
