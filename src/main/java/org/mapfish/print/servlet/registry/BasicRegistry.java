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

import java.util.HashMap;
import java.util.Map;


public class BasicRegistry implements Registry {

    private final Map<String, Object> registry = new HashMap<String, Object>();

    @Override
    public boolean containsKey(String key) {
        return registry.containsKey(key);
    }

    @Override
    public void setString(String key, String value) {
        registry.put(key, value);
    }

    @Override
    public String getString(String key) {
        return (String) registry.get(key);
    }

    @Override
    public void setInteger(String key, Integer value) {
        registry.put(key, value);
    }

    @Override
    public Integer getInteger(String key) {
        return (Integer) registry.get(key);
    }

    @Override
    public void setLong(String key, Long value) {
        registry.put(key, value);
    }

    @Override
    public Long getLong(String key) {
        return (Long) registry.get(key);
    }

    @Override
    public void setJSON(String key, JSONObject value) {
        registry.put(key, value);
    }

    @Override
    public JSONObject getJSON(String key) {
        return (JSONObject) registry.get(key);
    }

    @Override
    public void setBytes(String key, byte[] value) {
        registry.put(key, value);
    }

    @Override
    public byte[] getBytes(String key) {
        return (byte[]) registry.get(key);
    }
}
