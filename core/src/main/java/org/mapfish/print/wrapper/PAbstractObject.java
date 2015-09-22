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

package org.mapfish.print.wrapper;


/**
 * Abstract class for PObject implementation.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public abstract class PAbstractObject extends PElement implements PObject {

    /**
     * Constructor.
     *
     * @param parent the parent element
     * @param contextName the field name of this element in the parent.
     */
    public PAbstractObject(final PElement parent, final String contextName) {
        super(parent, contextName);
    }

    /**
     * Get a property as a string or throw an exception.
     * @param key the property name
     */
    @Override
    public final String getString(final String key) {
        String result = optString(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a string or defaultValue.
     * @param key the property name
     * @param defaultValue the default value
     */
    @Override
    public final String optString(final String key, final String defaultValue) {
        String result = optString(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Get a property as an int or throw an exception.
     * @param key the property name
     */
    @Override
    public final int getInt(final String key) {
        Integer result = optInt(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as an int or default value.
     * @param key the property name
     * @param defaultValue the default value
     */
    @Override
    public final Integer optInt(final String key, final Integer defaultValue) {
        Integer result = optInt(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Get a property as an long or throw an exception.
     * @param key the property name
     */
    @Override
    public final long getLong(final String key) {
        Long result = optLong(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }
    /**
     * Get a property as an long or default value.
     * @param key the property name
     * @param defaultValue the default value
     */
    @Override
    public final long optLong(final String key, final long defaultValue) {
        Long result = optLong(key);
        return result == null ? defaultValue : result;
    }
    /**
     * Get a property as a double or throw an exception.
     * @param key the property name
     */
    @Override
    public final double getDouble(final String key) {
        Double result = optDouble(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a double or defaultValue.
     * @param key the property name
     * @param defaultValue the default value
     */
    @Override
    public final Double optDouble(final String key, final Double defaultValue) {
        Double result = optDouble(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Get a property as a float or throw an exception.
     * @param key the property name
     */
    @Override
    public final float getFloat(final String key) {
        Float result = optFloat(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a float or Default value.
     * @param key the property name
     * @param defaultValue default value
     */
    @Override
    public final Float optFloat(final String key, final Float defaultValue) {
        Float result = optFloat(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Get a property as a boolean or throw exception.
     * @param key the property name
     */
    @Override
    public final boolean getBool(final String key) {
        Boolean result = optBool(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a boolean or default value.
     * @param key the property name
     * @param defaultValue the default
     */
    @Override
    public final Boolean optBool(final String key, final Boolean defaultValue) {
        Boolean result = optBool(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Get a property as a object or throw exception.
     * @param key the property name
     */
    @Override
    public final PObject getObject(final String key) {
        PObject result = optObject(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a array or default.
     * @param key the property name
     * @param defaultValue default
     */
    @Override
    public final PObject optObject(final String key, final PObject defaultValue) {
        PObject result = optObject(key);
        return result == null ? defaultValue : result;
    }

    /**
     * Get a property as a array or throw exception.
     * @param key the property name
     */
    @Override
    public final PArray getArray(final String key) {
        PArray result = optArray(key);
        if (result == null) {
            throw new ObjectMissingException(this, key);
        }
        return result;
    }

    /**
     * Get a property as a array or default.
     * @param key the property name
     * @param defaultValue default
     */
    @Override
    public final PArray optArray(final String key, final PArray defaultValue) {
        PArray result = optArray(key);
        return result == null ? defaultValue : result;
    }
}
