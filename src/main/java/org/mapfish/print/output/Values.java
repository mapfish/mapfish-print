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


public class Values {
    private final Map<String, Object> values;

    protected Values() {
        values = new HashMap<String, Object>();
    }

    public Values(Map<String, Object> values) {
        this.values = values;
    }

    protected void put(String key, Object value) {
        values.put(key, value);
    }

    protected Map<String, Object> getParameters() {
        return values;
    }

    public String getString(String key) {
        return (String) values.get(key);
    }

    public Double getDouble(String key) {
        return (Double) values.get(key);
    }

    public Integer getInteger(String key) {
        return (Integer) values.get(key);
    }

    public Object getObject(String key) {
        return values.get(key);
    }

    protected Iterable<Values> getIterator(String key) {
        return (Iterable<Values>) values.get(key);
    }
}
