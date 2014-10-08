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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.ExceptionUtils;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

/**
 * A simple implementation of {@link org.mapfish.print.servlet.registry.Registry} based on a
 * {@link java.util.HashMap}.
 */
public class BasicRegistry implements Registry {
    private static final int TIME_TO_KEEP_AFTER_ACCESS = 30;
    private Cache<String, Object> registry;
    private int timeToKeepAfterAccessInMinutes = TIME_TO_KEEP_AFTER_ACCESS;

    public final void setTimeToKeepAfterAccessInMinutes(final int timeToKeepAfterAccessInMinutes) {
        this.timeToKeepAfterAccessInMinutes = timeToKeepAfterAccessInMinutes;
    }

    @PostConstruct
    private void init() {
        this.registry = CacheBuilder.newBuilder().
                concurrencyLevel(1).
                expireAfterAccess(this.timeToKeepAfterAccessInMinutes, TimeUnit.MINUTES).build();
    }

    @Override
    public final long getTimeToKeepAfterAccessInMillis() {
        return TimeUnit.MINUTES.toMillis(this.timeToKeepAfterAccessInMinutes);
    }

    @Override
    public final synchronized boolean containsKey(final String key) {
        return this.registry.getIfPresent(key) != null;
    }

    @Override
    public final synchronized void put(final String key, final URI value) {
        this.registry.put(key, value);
    }

    @Override
    public final synchronized long incrementLong(final String key, final long amount) {
        long newValue = opt(key, amount);
        put(key, newValue);
        return newValue;
    }

    @Override
    public final synchronized int incrementInt(final String key, final int amount) {
        int newValue = opt(key, amount);
        put(key, newValue);
        return newValue;
    }

    @Override
    public final synchronized URI getURI(final String key) {
        return (URI) this.registry.getIfPresent(key);
    }

    @Override
    public final synchronized void put(final String key, final String value) {
        this.registry.put(key, value);
    }

    @Override
    public final synchronized String getString(final String key) {
        return (String) this.registry.getIfPresent(key);
    }

    @Override
    public final synchronized void put(final String key, final Number value) {
        this.registry.put(key, value);
    }

    @Override
    public final synchronized Number getNumber(final String key) {
        return (Number) this.registry.getIfPresent(key);
    }

    @Override
    public final synchronized <T> T opt(final String key, final T defaultValue) {
        @SuppressWarnings("unchecked") T value = (T) this.registry.getIfPresent(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public final synchronized void put(final String key, final JSONObject value) {
        // convert to string.  We don't want mutable values in the registry
        this.registry.put(key, value.toString());
    }

    @Override
    public final synchronized JSONObject getJSON(final String key) {
        String source;
        synchronized (this) {
            source = (String) this.registry.getIfPresent(key);
        }
        try {
            return new JSONObject(source);
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

}
