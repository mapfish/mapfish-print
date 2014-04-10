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
package org.mapfish.print.utils;

/**
 * Utility class for map blocks management.
 * 
 * @author mbarto
 *
 */
public class Maps {
    /**
     * Extracts a map configuration for the given map name.
     * 
     * @param parent
     * @param name
     * @return
     */
    public static PJsonObject getMapRoot(PJsonObject parent, String name) {
        // if we have multiple maps they are configured in a maps block
        PJsonObject maps = parent.optJSONObject("maps");
        if (maps != null && name != null) {
            if(maps.has(name)) { 
                return maps.getJSONObject(name);
            } else {
                throw new RuntimeException("Cannot find any map named " + name);
            }
        }
        return parent;
    }
}
