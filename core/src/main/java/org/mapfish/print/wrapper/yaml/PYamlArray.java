package org.mapfish.print.wrapper.yaml;

import org.json.JSONArray;
import org.json.JSONException;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonArray;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Array wrapper for Yaml parsing.
 */
public class PYamlArray extends PElement implements PArray {

    private final List<Object> array;

    /**
     * Constructor.
     *
     * @param parent the parent object.
     * @param array the array to wrap
     * @param contextName the name of this object within the parent.
     */
    public PYamlArray(final PElement parent, final List<Object> array, final String contextName) {
        super(parent, contextName);
        this.array = array;
    }

    @Override
    public final int size() {
        return this.array.size();
    }

    @Override
    public final PObject getObject(final int i) {
        @SuppressWarnings("unchecked")
        Map<String, Object> val = (Map<String, Object>) this.array.get(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new ObjectMissingException(this, context);
        }
        return new PYamlObject(this, val, context);
    }

    @Override
    public final PArray getArray(final int i) {
        @SuppressWarnings("unchecked")
        List<Object> val = (List<Object>) this.array.get(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new ObjectMissingException(this, context);
        }
        return new PYamlArray(this, val, context);
    }

    @Override
    public final int getInt(final int i) {
        return (Integer) this.array.get(i);
    }

    @Override
    public final long getLong(final int i) {
        Number result = (Number) this.array.get(i);
        return result.longValue();
    }

    @Override
    public final float getFloat(final int i) {
        Number result = (Number) this.array.get(i);
        return result.floatValue();
    }

    @Override
    public final double getDouble(final int i) {
        Number result = (Number) this.array.get(i);
        return result.doubleValue();
    }

    @Override
    public final String getString(final int i) {
        return (String) this.array.get(i);
    }

    @Override
    public final boolean getBool(final int i) {
        return (Boolean) this.array.get(i);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Object get(final int i) {
        final Object o = this.array.get(i);
        if (o instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) o;
            return new PYamlObject(this, map, String.valueOf(i));
        } else if (o instanceof List<?>) {
            List<Object> objects = (List<Object>) o;
            return new PYamlArray(this, objects, String.valueOf(i));
        } else if (o != null && o.getClass().isArray()) {
            List<Object> objects = Arrays.asList((Object[]) o);
            return new PYamlArray(this, objects, String.valueOf(i));
        }
        return o;
    }

    @Override
    public final String toString() {
        try {
            return "PYaml(" + getCurrentPath() + ":" + toJSON().getInternalArray().toString(2) + ")";
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    /**
     * Convert this object to a json array.
     */
    public final PJsonArray toJSON() {
        JSONArray jsonArray = new JSONArray();
        final int size = this.array.size();
        for (int i = 0; i < size; i++) {
            final Object o = get(i);
            if (o instanceof PYamlObject) {
                PYamlObject pYamlObject = (PYamlObject) o;
                jsonArray.put(pYamlObject.toJSON().getInternalObj());
            } else if (o instanceof PYamlArray) {
                PYamlArray pYamlArray = (PYamlArray) o;
                jsonArray.put(pYamlArray.toJSON().getInternalArray());
            } else {
                jsonArray.put(o);
            }

        }
        return new PJsonArray(null, jsonArray, getContextName());
    }
}
