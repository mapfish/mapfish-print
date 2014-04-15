/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General public final License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General public final License for more details.
 *
 * You should have received a copy of the GNU General public final License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.wrapper.yaml;

import org.mapfish.print.wrapper.PAbstractObject;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Object wrapper for Yaml parsing.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public class PYamlObject extends PAbstractObject {
    private final Map<String, Object> obj;

    /**
     * Constructor.
     *
     * @param obj the internal json element
     * @param contextName the field name of this element in the parent.
     */
    public PYamlObject(final Map<String, Object> obj, final String contextName) {
        this(null, obj, contextName);
    }

    /**
     * Constructor.
     *
     * @param parent the parent element
     * @param obj the internal json element
     * @param contextName the field name of this element in the parent.
     */
    public PYamlObject(final PElement parent, final Map<String, Object> obj, final String contextName) {
        super(parent, contextName);
        this.obj = obj;
    }

    @Override
    public final String optString(final String key) {
        return (String) this.obj.get(key);
    }

    @Override
    public final Integer optInt(final String key) {
        return (Integer) this.obj.get(key);
    }

    @Override
    public final Double optDouble(final String key) {
        Number result = (Number) this.obj.get(key);
        return result == null ? null : result.doubleValue();
    }

    @Override
    public final Float optFloat(final String key) {
        Number result = (Number) this.obj.get(key);
        return result == null ? null : result.floatValue();
    }

    @Override
    public final Boolean optBool(final String key) {
        return (Boolean) this.obj.get(key);
    }

    @Override
    public final PObject optObject(final String key) {
        final Map<String, Object> val = (Map<String, Object>) this.obj.get(key);
        return val == null ? null : new PYamlObject(this, val, key);
    }

    @Override
    public final PArray optArray(final String key) {
        final List<Object> val = (List<Object>) this.obj.get(key);
        return val == null ? null : new PYamlArray(this, val, key);
    }

    @Override
    public final Iterator<String> keys() {
        return this.obj.keySet().iterator();
    }

    @Override
    public final int size() {
        return this.obj.keySet().size();
    }

    @Override
    public final boolean has(final String key) {
        return this.obj.containsKey(key);
    }
}
