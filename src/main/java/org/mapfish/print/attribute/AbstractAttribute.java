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

import java.util.Collection;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONWriter;


public abstract class AbstractAttribute implements Attribute {
    protected LinkedHashMap<String, ?> clientOptions;

    protected abstract String getType();
    
    public void printClientConfig(JSONWriter json) throws JSONException {
        json.key("name").value(getType());
        additionaPrintClientConfig(json);
        json.key("clientOptions");
        addMapToJSON(clientOptions, json);
        json.endObject();
    }

    protected void additionaPrintClientConfig(JSONWriter json) throws JSONException {};

    @SuppressWarnings("unchecked")
    protected void addMapToJSON(LinkedHashMap<String, ?> map, JSONWriter json) throws JSONException {
        json.object();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof LinkedHashMap) {
                json.key(key);
                addMapToJSON((LinkedHashMap<String, ?>)value, json);
            }
            else if (value instanceof Collection<?>) {
                json.key(key);
                json.array();
                for (Object av : (Collection<?>)value) {
                    json.value(av);
                }
                json.endArray();
            }
            else if (value instanceof Integer) {
                json.key(key).value(((Integer)value).intValue());
            }
            else if (value instanceof Double) {
                json.key(key).value(((Double)value).doubleValue());
            }
            else {
                json.key(key).value(value);
            }
        }
        json.endObject();        
    }

    public LinkedHashMap<String, ?> getClientOptions() {
        return clientOptions;
    }

    public void setClientOptions(LinkedHashMap<String, ?> clientOptions) {
        this.clientOptions = clientOptions;
    }
}
