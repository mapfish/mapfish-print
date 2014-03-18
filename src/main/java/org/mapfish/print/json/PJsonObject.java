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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Wrapper around the {@link org.json.JSONObject} class to have a better
 * error management.
 */
public class PJsonObject extends PJsonElement {
    private final JSONObject obj;
    /**
     * Constructor.
     * 
     * @param obj the internal json element
     * @param contextName the field name of this element in the parent.
     */
    public PJsonObject(final JSONObject obj, final String contextName) {
        this(null, obj, contextName);
    }
    /**
     * Constructor.
     * 
     * @param parent the parent element
     * @param obj the internal json element
     * @param contextName the field name of this element in the parent.
     */
    public PJsonObject(final PJsonElement parent, final JSONObject obj, final String contextName) {
        super(parent, contextName);
        this.obj = obj;
    }

    /**
     * Get a property as a string or null.
     * @param key the property name
     */
    public final String optString(final String key) {
        return optString(key, null);
    }

    /**
     * Get a property as a string or defaultValue.
     * @param key the property name
     * @param defaultValue the default value
     */
    public final String optString(final String key, final String defaultValue) {
        return this.obj.optString(key, defaultValue);
    }
    
    /**
     * Get a property as a string or throw an exception.
     * @param key the property name
     */
    public final String getString(final String key) {
        String result = this.obj.optString(key, null);
        if (result == null) {
            throw new JsonMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a int or throw an exception.
     * @param key the property name
     */ 
    public final int getInt(final String key) {
        Integer result = this.obj.optInt(key, Integer.MIN_VALUE);
        if (result == Integer.MIN_VALUE) {
            throw new JsonMissingException(this, key);
        }
        return result;
    }
    
    /**
     * Get a property as a int or MIN_VALUE.
     * @param key the property name
     */
    public final Integer optInt(final String key) {
        final int result = this.obj.optInt(key, Integer.MIN_VALUE);
        return result == Integer.MIN_VALUE ? null : result;
    }
    /**
     * Get a property as a int or default value.
     * @param key the property name
     * @param defaultValue the default value
     */
    public final int optInt(final String key, final int defaultValue) {
        return this.obj.optInt(key, defaultValue);
    }

    /**
     * Get a property as a double or throw an exception.
     * @param key the property name
     */
    public final double getDouble(final String key) {
        double result = this.obj.optDouble(key, Double.NaN);
        if (Double.isNaN(result)) {
            throw new JsonMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a double or defaultValue.
     * @param key the property name
     * @param defaultValue the default value
     */
    public final double optDouble(final String key, final double defaultValue) {
        return this.obj.optDouble(key, defaultValue);
    }

    /**
     * Get a property as a float or throw an exception.
     * @param key the property name
     */
    public final float getFloat(final String key) {
        return (float) getDouble(key);
    }

    /**
     * Get a property as a float or null.
     * @param key the property name
     */
    public final Float optFloat(final String key) {
        double result = this.obj.optDouble(key, Double.NaN);
        if (Double.isNaN(result)) {
            return null;
        }
        return (float) result;
    }

    /**
     * Get a property as a float or Default vaule.
     * @param key the property name
     * @param defaultValue default value
     */
    public final Float optFloat(final String key, final float defaultValue) {
        return (float) this.obj.optDouble(key, defaultValue);
    }

    /**
     * Get a property as a boolean or throw exception.
     * @param key the property name
     */
    public final boolean getBool(final String key) {
        try {
            return this.obj.getBoolean(key);
        } catch (JSONException e) {
            throw new JsonMissingException(this, key);
        }
    }

    /**
     * Get a property as a boolean or null.
     * @param key the property name
     */
    public final Boolean optBool(final String key) {
        if (this.obj.optString(key) == null) {
            return null;
        } else {
            return this.obj.optBoolean(key);
        }
    }

    /**
     * Get a property as a boolean or default value.
     * @param key the property name
     * @param defaultValue the default
     */
    public final boolean optBool(final String key, final boolean defaultValue) {
        return this.obj.optBoolean(key, defaultValue);
    }

    /**
     * Get a property as a json object or null.
     * @param key the property name
     */
    public final PJsonObject optJSONObject(final String key) {
        final JSONObject val = this.obj.optJSONObject(key);
        return val != null ? new PJsonObject(this, val, key) : null;
    }

    /**
     * Get a property as a json object or throw exception.
     * @param key the property name
     */
    public final PJsonObject getJSONObject(final String key) {
        final JSONObject val = this.obj.optJSONObject(key);
        if (val == null) {
            throw new JsonMissingException(this, key);
        }
        return new PJsonObject(this, val, key);
    }

    /**
     * Get a property as a json array or throw exception.
     * @param key the property name
     */
    public final PJsonArray getJSONArray(final String key) {
        final JSONArray val = this.obj.optJSONArray(key);
        if (val == null) {
            throw new JsonMissingException(this, key);
        }
        return new PJsonArray(this, val, key);
    }

    /**
     * Get a property as a json array or null.
     * @param key the property name
     */
    public final PJsonArray optJSONArray(final String key) {
        final JSONArray val = this.obj.optJSONArray(key);
        if (val == null) {
            return null;
        }
        return new PJsonArray(this, val, key);
    }

    /**
     * Get a property as a json array or default.
     * @param key the property name
     * @param defaultValue default
     */
    public final PJsonArray optJSONArray(final String key, final PJsonArray defaultValue) {
        PJsonArray result = optJSONArray(key);
        return result != null ? result : defaultValue;
    }

    /**
     * Get an iterator of all keys in this objects.
     * @return
     */
    @SuppressWarnings("unchecked")
	public final Iterator<String> keys() {
        return this.obj.keys();
    }

    /**
     * Get the number of properties in this object.
     */
    public final int size() {
        return this.obj.length();
    }

    // CHECKSTYLE:OFF
    // Don't run checkstyle on generated methods
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PJsonObject other = (PJsonObject) obj;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		return true;
	}
	// CHECKSTYLE:ON
	
	/**
     * Get the internal json object.
     * @return
     */
    public final JSONObject getInternalObj() {
        return this.obj;
    }

    /**
     * Check if the object has a property with the key.
     * @param key key to check for.
     */
    public final boolean has(final String key) {
        String result = this.obj.optString(key, null);
        return result != null;
    }
}
