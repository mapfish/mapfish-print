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

package org.mapfish.print.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mapfish.print.JsonMissingException;

/**
 * Wrapper around the {@link org.json.JSONArray} class to have a better
 * error managment.
 */
public class PJsonArray extends PJsonElement {
    private final JSONArray array;

    public PJsonArray(PJsonElement parent, JSONArray array, String contextName) {
        super(parent, contextName);
        this.array = array;
    }

    public int size() {
        return array.length();
    }

    public PJsonObject getJSONObject(int i) {
        JSONObject val = array.optJSONObject(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new JsonMissingException(this, context);
        }
        return new PJsonObject(this, val, context);
    }

    public PJsonArray getJSONArray(int i) {
        JSONArray val = array.optJSONArray(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new JsonMissingException(this, context);
        }
        return new PJsonArray(this, val, context);
    }

    public int getInt(int i) {
        int val = array.optInt(i, Integer.MIN_VALUE);
        if (val == Integer.MIN_VALUE) {
            throw new JsonMissingException(this, "[" + i + "]");
        }
        return val;
    }

    public float getFloat(int i) {
        double val = array.optDouble(i, Double.MAX_VALUE);
        if (val == Double.MAX_VALUE) {
            throw new JsonMissingException(this, "[" + i + "]");
        }
        return (float) val;
    }

    public String getString(int i) {
        String val = array.optString(i, null);
        if (val == null) {
            throw new JsonMissingException(this, "[" + i + "]");
        }
        return val;
    }

    /**
     * @deprecated Use only if you know what you are doing! 
     */
    public JSONArray getInternalArray() {
        return array;
    }
}
