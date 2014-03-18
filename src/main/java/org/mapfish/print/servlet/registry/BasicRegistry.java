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

package org.mapfish.print.servlet.registry;

import org.json.JSONObject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of {@link org.mapfish.print.servlet.registry.Registry} based on a
 * {@link java.util.HashMap}.
 */
public class BasicRegistry implements Registry {

    private final Map<String, Object> registry = new HashMap<String, Object>();

    @Override
	public final synchronized boolean containsKey(final String key) {
        return this.registry.containsKey(key);
    }

    @Override
    public final synchronized void put(final String key, final URI value) {
        this.registry.put(key, value);
    }

    @Override
    public final long incrementLong(final String key, final long amount) {
        long newValue = amount;
        if (containsKey(key)) {
            newValue = getNumber(key).longValue() + amount;
        }
        put(key, newValue);
        return newValue;

    }

    @Override
    public final synchronized int incrementInt(final String key, final int amount) {
        int newValue = amount;
        if (containsKey(key)) {
            newValue = getNumber(key).intValue() + amount;
        }
        put(key, newValue);
        return newValue;
    }

    @Override
    public final synchronized URI getURI(final String key) {
        return (URI) this.registry.get(key);
    }

    @Override
	public final synchronized void put(final String key, final String value) {
        this.registry.put(key, value);
    }

    @Override
	public final synchronized String getString(final String key) {
        return (String) this.registry.get(key);
    }

    @Override
	public final synchronized void put(final String key, final Number value) {
        this.registry.put(key, value);
    }

    @Override
	public final synchronized Number getNumber(final String key) {
        return (Number) this.registry.get(key);
    }

    @Override
	public final synchronized void put(final String key, final JSONObject value) {
        this.registry.put(key, value);
    }

    @Override
	public final synchronized JSONObject getJSON(final String key) {
        return (JSONObject) this.registry.get(key);
    }

}
