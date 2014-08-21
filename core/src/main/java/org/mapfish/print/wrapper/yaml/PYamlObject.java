/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General public final License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General public final License for more details.
 *
 * You should have received a copy of the GNU General public final License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.wrapper.yaml;

import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.wrapper.PAbstractObject;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Object wrapper for Yaml parsing.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public class PYamlObject extends PAbstractObject {
    private final Map<String, Object> obj;

    /**
     * Constructor.
     *
     * @param obj         the internal json element
     * @param contextName the field name of this element in the parent.
     */
    public PYamlObject(final Map<String, Object> obj, final String contextName) {
        this(null, obj, contextName);
    }

    /**
     * Constructor.
     *
     * @param parent      the parent element
     * @param obj         the internal json element
     * @param contextName the field name of this element in the parent.
     */
    public PYamlObject(final PElement parent, final Map<String, Object> obj, final String contextName) {
        super(parent, contextName);
        this.obj = obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Object opt(final String key) {
        final Object value = this.obj.get(key);
        if (value instanceof Map<?, ?>) {
            return new PYamlObject(this, (Map<String, Object>) value, key);
        } else if (value instanceof List<?>) {
            return new PYamlArray(this, (List<Object>) value, key);
        } else if (value != null && value.getClass().isArray()) {
            return new PYamlArray(this, Arrays.asList((Object[]) value), key);
        } else {
            return value;
        }
    }

    @Override
    public final String optString(final String key) {
        return (String) this.obj.get(key);
    }

    @Override
    public final Integer optInt(final String key) {
        return (Integer) this.obj.get(key);
    }

    @Override
    public final Double optDouble(final String key) {
        Number result = (Number) this.obj.get(key);
        return result == null ? null : result.doubleValue();
    }

    @Override
    public final Float optFloat(final String key) {
        Number result = (Number) this.obj.get(key);
        return result == null ? null : result.floatValue();
    }

    @Override
    public final Boolean optBool(final String key) {
        return (Boolean) this.obj.get(key);
    }

    @Override
    public final PObject optObject(final String key) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> val = (Map<String, Object>) this.obj.get(key);
        return val == null ? null : new PYamlObject(this, val, key);
    }

    @Override
    public final PArray optArray(final String key) {
        @SuppressWarnings("unchecked")
        final List<Object> val = (List<Object>) this.obj.get(key);
        return val == null ? null : new PYamlArray(this, val, key);
    }

    @Override
    public final boolean isArray(final String key) {
        return this.obj.get(key) instanceof List<?>;
    }

    @Override
    public final Iterator<String> keys() {
        return this.obj.keySet().iterator();
    }

    @Override
    public final int size() {
        return this.obj.keySet().size();
    }

    @Override
    public final boolean has(final String key) {
        return this.obj.containsKey(key);
    }

    @Override
    public final String toString() {
        try {
            return "PYaml(" + this.getCurrentPath() + ":" + toJSON().getInternalObj().toString(2) + ")";
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    /**
     * Convert this object to a json object.
     */
    public final PJsonObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            for (String key : this.obj.keySet()) {
                Object opt = opt(key);
                if (opt instanceof PYamlObject) {
                    opt = ((PYamlObject) opt).toJSON().getInternalObj();
                } else if (opt instanceof PYamlArray) {
                    opt = ((PYamlArray) opt).toJSON().getInternalArray();
                }
                json.put(key, opt);
            }
            return new PJsonObject(json, this.getContextName());
        } catch (Throwable e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }
}
