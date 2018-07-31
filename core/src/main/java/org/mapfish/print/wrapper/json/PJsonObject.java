package org.mapfish.print.wrapper.json;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PAbstractObject;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;

import java.util.Iterator;

/**
 * Wrapper around the {@link org.json.JSONObject} class to have a better error management.
 */
public class PJsonObject extends PAbstractObject {
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
    public PJsonObject(final PElement parent, final JSONObject obj, final String contextName) {
        super(parent, contextName);
        this.obj = obj;
    }

    @Override
    public final Object opt(final String key) {
        return this.obj.opt(key);
    }

    /**
     * Get a property as a string or null.
     *
     * @param key the property name
     */
    @Override
    public final String optString(final String key) {
        return this.obj.optString(key, null);
    }

    /**
     * Get a property as a int or null.
     *
     * @param key the property name
     */
    @Override
    public final Integer optInt(final String key) {
        final int result = this.obj.optInt(key, Integer.MIN_VALUE);
        return result == Integer.MIN_VALUE ? null : result;
    }

    @Override
    public final Long optLong(final String key) {
        final long result = this.obj.optLong(key, Long.MIN_VALUE);
        return result == Long.MIN_VALUE ? null : result;
    }

    /**
     * Get a property as a double or null.
     *
     * @param key the property name
     */
    @Override
    public final Double optDouble(final String key) {
        double result = this.obj.optDouble(key, Double.NaN);
        if (Double.isNaN(result)) {
            return null;
        }
        return result;
    }

    /**
     * Get a property as a float or null.
     *
     * @param key the property name
     */
    @Override
    public final Float optFloat(final String key) {
        double result = this.obj.optDouble(key, Double.NaN);
        if (Double.isNaN(result)) {
            return null;
        }
        return (float) result;
    }

    /**
     * Get a property as a boolean or null.
     *
     * @param key the property name
     */
    @Override
    public final Boolean optBool(final String key) {
        if (this.obj.optString(key, null) == null) {
            return null;
        } else {
            return this.obj.optBoolean(key);
        }
    }

    /**
     * Get a property as a object or null.
     *
     * @param key the property name
     */
    @Override
    public final PObject optObject(final String key) {
        return optJSONObject(key);
    }

    /**
     * Get a property as a array or null.
     *
     * @param key the property name
     */
    @Override
    public final PArray optArray(final String key) {
        return optJSONArray(key);
    }

    @Override
    public final boolean isArray(final String key) {
        return getInternalObj().opt(key) instanceof JSONArray;
    }

    /**
     * Get a property as a json object or null.
     *
     * @param key the property name
     */
    public final PJsonObject optJSONObject(final String key) {
        final JSONObject val = this.obj.optJSONObject(key);
        return val != null ? new PJsonObject(this, val, key) : null;
    }

    /**
     * Get a property as a json object or throw exception.
     *
     * @param key the property name
     */
    public final PJsonObject getJSONObject(final String key) {
        final JSONObject val = this.obj.optJSONObject(key);
        if (val == null) {
            throw new ObjectMissingException(this, key);
        }
        return new PJsonObject(this, val, key);
    }

    /**
     * Get a property as a json array or throw exception.
     *
     * @param key the property name
     */
    public final PJsonArray getJSONArray(final String key) {
        final JSONArray val = this.obj.optJSONArray(key);
        if (val == null) {
            throw new ObjectMissingException(this, key);
        }
        return new PJsonArray(this, val, key);
    }

    /**
     * Get a property as a json array or null.
     *
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
     *
     * @param key the property name
     * @param defaultValue default
     */
    public final PJsonArray optJSONArray(final String key, final PJsonArray defaultValue) {
        PJsonArray result = optJSONArray(key);
        return result != null ? result : defaultValue;
    }

    /**
     * Get an iterator of all keys in this objects.
     *
     * @return The keys iterator
     */
    @Override
    @SuppressWarnings("unchecked")
    public final Iterator<String> keys() {
        return this.obj.keys();
    }

    /**
     * Get the number of properties in this object.
     */
    @Override
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PJsonObject other = (PJsonObject) obj;
        if (this.obj == null) {
            if (other.obj != null) {
                return false;
            }
        } else if (!this.obj.equals(other.obj)) {
            return false;
        }
        return true;
    }
    // CHECKSTYLE:ON

    /**
     * Get the internal json object.
     *
     * @return The internal object
     */
    public final JSONObject getInternalObj() {
        return this.obj;
    }

    /**
     * Check if the object has a property with the key.
     *
     * @param key key to check for.
     */
    @Override
    public final boolean has(final String key) {
        String result = this.obj.optString(key, null);
        return result != null;
    }

    @Override
    public final String toString() {
        return getCurrentPath() + ":\n\t" + this.obj;
    }
}
