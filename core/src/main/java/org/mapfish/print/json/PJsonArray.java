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

package org.mapfish.print.json;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Wrapper around the {@link org.json.JSONArray} class to have a better
 * error management.
 */
public class PJsonArray extends PJsonElement {
    private final JSONArray array;

    /**
     * Constructor.
     * @param parent the parent object.
     * @param array the array to wrap
     * @param contextName the name of this object within the parent.
     */
    public PJsonArray(final PJsonElement parent, final JSONArray array, final String contextName) {
        super(parent, contextName);
        this.array = array;
    }

    /**
     * Return the size of the array.
     */
    public final int size() {
        return this.array.length();
    }

    /**
     * Get the element at the index as a json object.
     * @param i the index of the object to access
     */
    public final PJsonObject getJSONObject(final int i) {
        JSONObject val = this.array.optJSONObject(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new JsonMissingException(this, context);
        }
        return new PJsonObject(this, val, context);
    }

    /**
     * Get the element at the index as a json array.
     * @param i the index of the element to access
     */
    public final PJsonArray getJSONArray(final int i) {
        JSONArray val = this.array.optJSONArray(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new JsonMissingException(this, context);
        }
        return new PJsonArray(this, val, context);
    }

    /**
     * Get the element at the index as an integer.
     * @param i the index of the element to access
     */
    public final int getInt(final int i) {
        int val = this.array.optInt(i, Integer.MIN_VALUE);
        if (val == Integer.MIN_VALUE) {
            throw new JsonMissingException(this, "[" + i + "]");
        }
        return val;
    }

    /**
     * Get the element at the index as a float.
     * @param i the index of the element to access
     */
    public final float getFloat(final int i) {
        double val = this.array.optDouble(i, Double.MAX_VALUE);
        if (val == Double.MAX_VALUE) {
            throw new JsonMissingException(this, "[" + i + "]");
        }
        return (float) val;
    }

    /**
     * Get the element at the index as a double.
     * @param i the index of the element to access
     */
    public final double getDouble(final int i) {
        double val = this.array.optDouble(i, Double.MAX_VALUE);
        if (val == Double.MAX_VALUE) {
            throw new JsonMissingException(this, "[" + i + "]");
        }
        return val;
    }

    /**
     * Get the element at the index as a string.
     * @param i the index of the element to access
     */
    public final String getString(final int i) {
        String val = this.array.optString(i, null);
        if (val == null) {
            throw new JsonMissingException(this, "[" + i + "]");
        }
        return val;
    }

    /**
     * @deprecated Use only if you know what you are doing!
     */
    public final JSONArray getInternalArray() {
        return this.array;
    }
}
