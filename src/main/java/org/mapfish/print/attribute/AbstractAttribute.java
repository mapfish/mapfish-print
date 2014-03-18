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

package org.mapfish.print.attribute;

import org.json.JSONException;
import org.json.JSONWriter;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * An attribute containing useful methods for other attribute implementation as well as some basic functionality.
 *
 * @param <T> The attribute type object read from the parameters by this attribute.
 */
public abstract class AbstractAttribute<T> implements Attribute<T> {
    private LinkedHashMap<String, ?> clientOptions;

    /**
     * Return a string describing the attribute's type. The string is used in the {@link #printClientConfig(org.json.JSONWriter)}.
     *
     * @return a string describing the attribute's type.
     */
    protected abstract String getType();

    @Override
    public final void printClientConfig(final JSONWriter json) throws JSONException {
        json.key("name").value(getType());
        additionalPrintClientConfig(json);
        json.key("clientOptions");
        addMapToJSON(this.clientOptions, json);
        json.endObject();
    }

    /**
     * Hook to add extra custom information to the normal printClientConfig method.
     *
     * @param json the json writer to write to.
     * @throws JSONException
     */
    protected void additionalPrintClientConfig(final JSONWriter json) throws JSONException {
    }

    /**
     * Utility method for adding a map of data to the json.
     *
     * @param map  the map of data to add to the json writer.
     * @param json writer to write to.
     * @throws JSONException
     */
    @SuppressWarnings("unchecked")
	protected final void addMapToJSON(final LinkedHashMap<String, ?> map, final JSONWriter json) throws JSONException {
        json.object();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof LinkedHashMap) {
                json.key(key);
                addMapToJSON((LinkedHashMap<String, ?>) value, json);
            } else if (value instanceof Collection<?>) {
                json.key(key);
                json.array();
                for (Object av : (Collection<?>) value) {
                    json.value(av);
                }
                json.endArray();
            } else if (value instanceof Integer) {
                json.key(key).value(((Integer) value).intValue());
            } else if (value instanceof Double) {
                json.key(key).value(((Double) value).doubleValue());
            } else {
                json.key(key).value(value);
            }
        }
        json.endObject();
    }

    /**
     * Setter called by the yaml parser.
     *
     * @param clientOptions the options.
     */
    public final void setClientOptions(final LinkedHashMap<String, ?> clientOptions) {
        this.clientOptions = clientOptions;
    }
}
