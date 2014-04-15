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

package org.mapfish.print.wrapper.yaml;

import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;

import java.util.List;
import java.util.Map;


/**
 * Array wrapper for Yaml parsing.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public class PYamlArray extends PElement implements PArray {

    private final List<Object> array;

    /**
     * Constructor.
     * @param parent the parent object.
     * @param array the array to wrap
     * @param contextName the name of this object within the parent.
     */
    public PYamlArray(final PElement parent, final List<Object> array, final String contextName) {
        super(parent, contextName);
        this.array = array;
    }

    @Override
    public final int size() {
        return this.array.size();
    }

    @Override
    public final PObject getObject(final int i) {
        Map<String, Object> val = (Map<String, Object>) this.array.get(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new ObjectMissingException(this, context);
        }
        return new PYamlObject(this, val, context);
    }

    @Override
    public final PArray getArray(final int i) {
        List<Object> val = (List<Object>) this.array.get(i);
        final String context = "[" + i + "]";
        if (val == null) {
            throw new ObjectMissingException(this, context);
        }
        return new PYamlArray(this, val, context);
    }

    @Override
    public final int getInt(final int i) {
        return (Integer) this.array.get(i);
    }

    @Override
    public final float getFloat(final int i) {
        Number result = (Number) this.array.get(i);
        return result.floatValue();
    }

    @Override
    public final double getDouble(final int i) {
        Number result = (Number) this.array.get(i);
        return result.doubleValue();
    }

    @Override
    public final String getString(final int i) {
        return (String) this.array.get(i);
    }

    @Override
    public final boolean getBool(final int i) {
        return (Boolean) this.array.get(i);
    }

}
