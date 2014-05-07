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

/**
 * A variable store for sharing common values across all nodes in a cluster.
 * <p>
 * For example the PDF key and URI might be put in this registry.
 * Also queue length perhaps.
 * </p>
 */
public interface Registry {

    /**
     * Check if something is registered for the key.
     * @param key key to check for
     */
    boolean containsKey(String key);

    /**
     * Put a URI in the registry.
     * @param key the key of the entry
     * @param value the value
     */
    void put(String key, URI value);

    /**
     * Get a URI from the registry.
     * @param key the key to use for lookup.
     */
    URI getURI(String key);

    /**
     * Put a string in the registry.
     * @param key the key of the entry
     * @param value the value
     */
    void put(String key, String value);

    /**
     * Get string from the registry.
     * @param key the key to use for lookup.
     */
    String getString(String key);

    /**
     * Put a number in the registry.
     * @param key the key of the entry
     * @param value the value
     */
    void put(String key, Number value);

    /**
     * Get a number from the registry.
     *
     * @param key the number
     */
    Number getNumber(String key);

    /**
     * Get a value from the registry or return the default if the value is not in the registry.
     *
     * @param key the key of the element to the key to use for lookup.
     * @param defaultValue the value to return if a value with the key is not in the registry
     * @param <T> the type of the returned object
     */
    <T> T opt(String key, T defaultValue);

    /**
     * Put a json object in the registry.
     *
     * @param key the key of the entry
     * @param value the value
     */
    void put(String key, JSONObject value);

    /**
     * Get a json object from the registry.
     *
     * @param key the key use for lookup
     */
    JSONObject getJSON(String key);

    /**
     * Increment the value currently stored in the registry by the amount.  This assumes it is an integer.
     *
     * If there is not value present in registry then a value will be registered as amount.
     *
     *
     * @param key the key of the element to increment
     *
     * @param amount th amount to increment
     * @return the new value
     */
    int incrementInt(String key, int amount);

    /**
     * Increment the value currently stored in the registry by the amount.  This assumes it is an long.
     *
     * If there is not value present in registry then a value will be registered as amount.
     *
     *
     * @param key the key of the element to increment
     *
     * @param amount the amount to increment
     * @return the new value
     */
    long incrementLong(String key, long amount);
}
