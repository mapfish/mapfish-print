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

package org.mapfish.print.servlet.registry;

import org.json.JSONObject;

/**
 * A variable store for sharing common values across all nodes in a cluster.
 * <p>
 * For example the PDF key and URI might be put in this registry.
 * Also queue length perhaps.
 * </p>
 */
public interface Registry {

    boolean containsKey(String key);

    void setString(String key, String value);

    String getString(String key);

    void setInteger(String key, Integer value);

    Integer getInteger(String key);

    void setLong(String key, Long value);

    Long getLong(String key);

    void setJSON(String key, JSONObject value);

    JSONObject getJSON(String key);

    void setBytes(String key, byte[] value);

    byte[] getBytes(String key);
}
