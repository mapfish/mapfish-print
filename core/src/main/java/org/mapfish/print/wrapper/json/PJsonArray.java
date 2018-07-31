package org.mapfish.print.wrapper.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;

/**
 * Wrapper around the {@link org.json.JSONArray} class to have a better error management.
 */
public class PJsonArray extends PElement implements PArray {
    private final JSONArray array;

    /**
     * Constructor.
     *
     * @param parent the parent object.
     * @param array the array to wrap
     * @param contextName the name of this object within the parent.
     */
    public PJsonArray(final PElement parent, final JSONArray array, final String contextName) {
        super(parent, contextName);
        this.array = array;
    }

    /**
     * Return the size of the array.
     */
    @Override
    public final int size() {
        return this.array.length();
    }

    /**
     * Get the element at the index as a json object.
     *
     * @param i the index of the object to access
     */
    @Override
    public final PObject getObject(final int i) {
        return getJSONObject(i);
    }

    /**
     * Get the element at the index as a json object.
     *
     * @param i the index of the object to access
     */
    public final PJsonObject getJSONObject(final int i) {
        JSONObject val = this.array.optJSONObject(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new ObjectMissingException(this, context);
        }
        return new PJsonObject(this, val, context);
    }

    /**
     * Get the element at the index as a json array.
     *
     * @param i the index of the element to access
     */
    @Override
    public final PArray getArray(final int i) {
        return getJSONArray(i);
    }

    /**
     * Get the element at the index as a json array.
     *
     * @param i the index of the element to access
     */
    public final PJsonArray getJSONArray(final int i) {
        JSONArray val = this.array.optJSONArray(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new ObjectMissingException(this, context);
        }
        return new PJsonArray(this, val, context);
    }

    /**
     * Get the element at the index as an integer.
     *
     * @param i the index of the element to access
     */
    @Override
    public final int getInt(final int i) {
        int val = this.array.optInt(i, Integer.MIN_VALUE);
        if (val == Integer.MIN_VALUE) {
            throw new ObjectMissingException(this, "[" + i + "]");
        }
        return val;
    }

    @Override
    public final long getLong(final int i) {
        long val = this.array.optLong(i, Long.MIN_VALUE);
        if (val == Long.MIN_VALUE) {
            throw new ObjectMissingException(this, "[" + i + "]");
        }
        return val;
    }

    /**
     * Get the element at the index as a float.
     *
     * @param i the index of the element to access
     */
    @Override
    public final float getFloat(final int i) {
        double val = this.array.optDouble(i, Double.MAX_VALUE);
        if (val == Double.MAX_VALUE) {
            throw new ObjectMissingException(this, "[" + i + "]");
        }
        return (float) val;
    }

    /**
     * Get the element at the index as a double.
     *
     * @param i the index of the element to access
     */
    @Override
    public final double getDouble(final int i) {
        double val = this.array.optDouble(i, Double.MAX_VALUE);
        if (val == Double.MAX_VALUE) {
            throw new ObjectMissingException(this, "[" + i + "]");
        }
        return val;
    }

    /**
     * Get the element at the index as a string.
     *
     * @param i the index of the element to access
     */
    @Override
    public final String getString(final int i) {
        String val = this.array.optString(i, null);
        if (val == null) {
            throw new ObjectMissingException(this, "[" + i + "]");
        }
        return val;
    }

    /**
     * Get access to underlying array.
     */
    public final JSONArray getInternalArray() {
        return this.array;
    }

    /**
     * Get the element as a boolean.
     *
     * @param i the index of the element to access
     */
    @Override
    public final boolean getBool(final int i) {
        try {
            return this.array.getBoolean(i);
        } catch (JSONException e) {
            throw new ObjectMissingException(this, "[" + i + "]");
        }
    }

    @Override
    public final Object get(final int i) {
        try {
            return this.array.get(i);
        } catch (JSONException e) {
            throw new ObjectMissingException(this, "[" + i + "]");
        }
    }

    @Override
    public final String toString() {
        return getCurrentPath() + ":\n\t" + this.array;
    }
}
