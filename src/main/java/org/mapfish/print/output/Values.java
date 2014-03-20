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

import java.util.HashMap;
import java.util.Map;

/**
 * Values that go into a processor from previous processors in the processor processing graph.
 * @author Jesse
 *
 */
public class Values {
    private final Map<String, Object> values = new HashMap<String, Object>();

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
     * Put a new value in map.
     *
     * @param key id of the value for looking up.
     * @param value the value.
     */
    protected final void put(final String key, final Object value) {
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
     * Get an a value as an interator of values.
     *
     * @param key the key
     */
    @SuppressWarnings("unchecked")
    protected final Iterable<Values> getIterator(final String key) {
        return (Iterable<Values>) this.values.get(key);
    }
}
