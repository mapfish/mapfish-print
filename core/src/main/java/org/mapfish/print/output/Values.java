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

package org.mapfish.print.output;

import org.mapfish.print.attribute.ArrayReflectiveAttribute;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.PrimitiveAttribute;
import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.json.parser.MapfishJsonParser;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Values that go into a processor from previous processors in the processor processing graph.
 * @author Jesse
 *
 */
public class Values {
    private final Map<String, Object> values = new ConcurrentHashMap<String, Object>();

    /**
     * Constructor.
     *
     * @param values initial values.
     */
    public Values(final Map<String, Object> values) {
        this.values.putAll(values);
    }

    /**
     * Constructor.
     */
    public Values() {
        // nothing to do
    }

    /**
     * Construct from the json request body and the associated template.
     *
     * @param requestData the json request data
     * @param template the template
     * @param parser the parser to use for parsing the request data.
     */
    public Values(final PJsonObject requestData, final Template template, final MapfishJsonParser parser) {

        final PJsonObject jsonAttributes = requestData.getJSONObject("attributes");

        Map<String, Attribute> attributes = template.getAttributes();
        for (String attributeName : attributes.keySet()) {
            final Attribute attribute = attributes.get(attributeName);
            final Object value;
            if (attribute instanceof PrimitiveAttribute) {
                PrimitiveAttribute pAtt = (PrimitiveAttribute) attribute;
                value = parser.parsePrimitive(attributeName, pAtt.getValueClass(), jsonAttributes);
            } else if (attribute instanceof ReflectiveAttribute) {
                boolean errorOnExtraParameters = template.getConfiguration().isThrowErrorOnExtraParameters();
                ReflectiveAttribute rAtt = (ReflectiveAttribute) attribute;
                value = rAtt.createValue(template);
                parser.parse(errorOnExtraParameters, jsonAttributes.getJSONObject(attributeName), value);
            } else if (attribute instanceof ArrayReflectiveAttribute) {
                boolean errorOnExtraParameters = template.getConfiguration().isThrowErrorOnExtraParameters();
                ArrayReflectiveAttribute rAtt = (ArrayReflectiveAttribute) attribute;
                final PJsonArray jsonArray = jsonAttributes.getJSONArray(attributeName);
                value = Array.newInstance(rAtt.createValue(template).getClass(), jsonArray.size());
                for (int i = 0; i < jsonArray.size(); i++) {
                    Object elem = rAtt.createValue(template);
                    Array.set(value, i, elem);
                    parser.parse(errorOnExtraParameters, jsonArray.getJSONObject(i), elem);
                }
            } else {
                throw new IllegalArgumentException("Unsupported attribute type: " + attribute);
            }

            put(attributeName, value);
        }
    }

    /**
     * Put a new value in map.
     *
     * @param key id of the value for looking up.
     * @param value the value.
     */
    public final void put(final String key, final Object value) {
        this.values.put(key, value);
    }

    /**
     * Get all parameters.
     */
    protected final Map<String, Object> getParameters() {
        return this.values;
    }

    /**
     * Get a value as a string.
     *
     * @param key the key for looking up the value.
     */
    public final String getString(final String key) {
        return (String) this.values.get(key);
    }

    /**
     * Get a value as a double.
     *
     * @param key the key for looking up the value.
     */
    public final Double getDouble(final String key) {
        return (Double) this.values.get(key);
    }

    /**
     * Get a value as a integer.
     *
     * @param key the key for looking up the value.
     */
    public final Integer getInteger(final String key) {
        return (Integer) this.values.get(key);
    }

    /**
     * Get a value as a string.
     *
     * @param key the key for looking up the value.
     * @param type the type of the object
     * @param <V> the type
     *
     */
    public final <V> V getObject(final String key, final Class<V> type) {
        return type.cast(this.values.get(key));
    }

    /**
     * Get an a value as an iterator of values.
     *
     * @param key the key
     */
    @SuppressWarnings("unchecked")
    public final Iterable<Values> getIterator(final String key) {
        return (Iterable<Values>) this.values.get(key);
    }

    /**
     * Return true if the identified value is present in this values.
     *
     * @param key the key to check for.
     */
    public final boolean containsKey(final String key) {
        return this.values.containsKey(key);
    }

    /**
     * Get a boolean value from the values or null.
     *
     * @param key the look up key of the value
     */
    @Nullable
    public final Boolean getBoolean(@Nonnull final String key) {
        return (Boolean) this.values.get(key);
    }

    /**
     * Remove a value from this object.
     *
     * @param key key of entry to remove.
     */
    public final void remove(final String key) {
        this.values.remove(key);
    }
}
