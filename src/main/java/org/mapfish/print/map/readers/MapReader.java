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

import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.readers.google.GoogleMapReader;
import org.mapfish.print.map.readers.google.GoogleMapTileReader;
import org.mapfish.print.utils.PJsonObject;

import java.util.List;

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

    public static void create(List<MapReader> target, String type, RenderingContext context, PJsonObject params) {
        // TODO add plugin architecture.  considering looking at OSGI
        if ("WMS".equalsIgnoreCase(type)) {
            WMSMapReader.create(target, context, params);
        } else if ("MapServer".equalsIgnoreCase(type)) {
            MapServerMapReader.create(target, context, params);
        } else if ("TileCache".equalsIgnoreCase(type)) {
            TileCacheMapReader.create(target, context, params);
        } else if ("Osm".equalsIgnoreCase(type)) {
            OsmMapReader.create(target, context, params);
        } else if ("Tms".equalsIgnoreCase(type)) {
            TmsMapReader.create(target, context, params);
        } else if ("Vector".equalsIgnoreCase(type)) {
            VectorMapReader.create(target, context, params);
        } else if ("Image".equalsIgnoreCase(type)) {
            ImageMapReader.create(target, context, params);
        } else if ("TiledGoogle".equalsIgnoreCase(type)) {
            GoogleMapTileReader.create(target, context, params);
        } else if ("Google".equalsIgnoreCase(type)) {
            GoogleMapReader.create(target, context, params);
        } else if ("KaMapCache".equalsIgnoreCase(type)) {
            KaMapCacheMapReader.create(target, context, params);
        } else if ("KaMap".equalsIgnoreCase(type)) {
            KaMapMapReader.create(target, context, params);
        } else {
            throw new InvalidJsonValueException(params, "type", type);
        }
    }

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
