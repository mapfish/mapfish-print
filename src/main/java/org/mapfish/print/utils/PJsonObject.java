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
import org.json.JSONException;
import org.mapfish.print.JsonMissingException;

import java.util.Iterator;

/**
 * Wrapper around the {@link org.json.JSONObject} class to have a better
 * error managment.
 */
public class PJsonObject extends PJsonElement {
    private final JSONObject obj;

    public PJsonObject(JSONObject obj, String contextName) {
        this(null, obj, contextName);
    }

    public PJsonObject(PJsonElement parent, JSONObject obj, String contextName) {
        super(parent, contextName);
        this.obj = obj;
    }

    public String optString(String key) {
        return optString(key, null);
    }

    public String optString(String key, String defaultValue) {
        return obj.optString(key, defaultValue);
    }

    public String getString(String key) {
        String result = obj.optString(key, null);
        if (result == null) {
            throw new JsonMissingException(this, key);
        }
        return result;
    }

    public int getInt(String key) {
        Integer result = obj.optInt(key, Integer.MIN_VALUE);
        if (result == Integer.MIN_VALUE) {
            throw new JsonMissingException(this, key);
        }
        return result;
    }

    public Integer optInt(String key) {
        final int result = obj.optInt(key, Integer.MIN_VALUE);
        return result == Integer.MIN_VALUE ? null : result;
    }

    public int optInt(String key, int defaultValue) {
        return obj.optInt(key, defaultValue);
    }

    public double getDouble(String key) {
        double result = obj.optDouble(key, Double.NaN);
        if (Double.isNaN(result)) {
            throw new JsonMissingException(this, key);
        }
        return result;
    }

    public float getFloat(String key) {
        return (float) getDouble(key);
    }

    public Float optFloat(String key) {
        double result = obj.optDouble(key, Double.NaN);
        if (Double.isNaN(result)) {
            return null;
        }
        return (float) result;
    }

    public Float optFloat(String key, float defaultValue) {
        return (float) obj.optDouble(key, defaultValue);
    }

    public boolean getBool(String key) {
        try {
            return obj.getBoolean(key);
        } catch (JSONException e) {
            throw new JsonMissingException(this, key);
        }
    }

    public Boolean optBool(String key) {
        if (obj.optString(key) == null) {
            return null;
        } else {
            return obj.optBoolean(key);
        }
    }

    public boolean optBool(String key, boolean defaultValue) {
        return obj.optBoolean(key, defaultValue);
    }

    public PJsonObject optJSONObject(String key) {
        final JSONObject val = obj.optJSONObject(key);
        return val != null ? new PJsonObject(this, val, key) : null;
    }

    public PJsonObject getJSONObject(String key) {
        final JSONObject val = obj.optJSONObject(key);
        if (val == null) {
            throw new JsonMissingException(this, key);
        }
        return new PJsonObject(this, val, key);
    }

    public PJsonArray getJSONArray(String key) {
        final JSONArray val = obj.optJSONArray(key);
        if (val == null) {
            throw new JsonMissingException(this, key);
        }
        return new PJsonArray(this, val, key);
    }

    public PJsonArray optJSONArray(String key) {
        final JSONArray val = obj.optJSONArray(key);
        if (val == null) {
            return null;
        }
        return new PJsonArray(this, val, key);
    }

    public PJsonArray optJSONArray(String key, PJsonArray defaultValue) {
        PJsonArray result = optJSONArray(key);
        return result != null ? result : defaultValue;
    }

    public Iterator<String> keys() {
        return obj.keys();
    }

    public int size() {
        return obj.length();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PJsonObject) {
            PJsonObject other = (PJsonObject) obj;
            if (size() != other.size()) {
                return false;
            }
            final Iterator<String> iterator = keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (!other.getString(key).equals(getString(key))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @deprecated Use only if you know what you are doing!
     */
    public JSONObject getInternalObj() {
        return obj;
    }

    public boolean has(String key) {
        String result = obj.optString(key, null);
        if (result == null) {
            return false;
        } else {
            return true;
        }
    }
}
