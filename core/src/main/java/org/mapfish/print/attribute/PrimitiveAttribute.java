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
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.MapfishParser;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A type of attribute whose value is a primitive type.
 * <ul>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.lang.Integer}</li>
 * <li>{@link java.lang.Float}</li>
 * <li>{@link java.lang.Double}</li>
 * <li>{@link java.lang.Short}</li>
 * <li>{@link java.lang.Boolean}</li>
 * <li>{@link java.lang.Character}</li>
 * <li>{@link java.lang.Byte}</li>
 * <li>{@link java.lang.Enum}</li>
 * </ul>
 *
 * @param <Value> The value type of the attribute
 * @author Jesse on 4/9/2014.
 */
public abstract class PrimitiveAttribute<Value> implements Attribute {
    private Class<Value> valueClass;
    private LinkedHashMap<String, ?> clientOptions;

    /**
     * Constructor.
     *
     * @param valueClass the type of the value of this attribute
     */
    public PrimitiveAttribute(final Class<Value> valueClass) {
        this.valueClass = valueClass;
    }

    public final Class<Value> getValueClass() {
        return this.valueClass;
    }

    @Override
    public void validate(final List<Throwable> validationErrors) {
        // no checks required
    }

    @Override
    public final void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        json.key("name").value(MapfishParser.stringRepresentation(this.valueClass));
        if (this.clientOptions != null) {
            addMapToJSON("clientOptions", this.clientOptions, json);
        }
    }

    /**
     * Setter called by the yaml parser.   Client options provides extra, non-standard information for the client.  These
     * should be kept to a minimum.
     *
     * @param clientOptions the options.
     */
    public final void setClientOptions(final LinkedHashMap<String, ?> clientOptions) {
        this.clientOptions = clientOptions;
    }


    /**
     * Utility method for adding a map of data to the json.
     *
     * @param jsonKey the json key of the object to be added
     * @param map     the map of data to add to the json writer.
     * @param json    writer to write to.
     * @throws JSONException
     */
    @SuppressWarnings("unchecked")
    protected final void addMapToJSON(final String jsonKey, final LinkedHashMap<String, ?> map, final JSONWriter json)
            throws JSONException {
        json.key(jsonKey);
        json.object();
        if (map != null) {
            for (String key : map.keySet()) {
                Object value = map.get(key);
                if (value instanceof LinkedHashMap) {
                    addMapToJSON(key, (LinkedHashMap<String, ?>) value, json);
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
        }
        json.endObject();
    }

}
