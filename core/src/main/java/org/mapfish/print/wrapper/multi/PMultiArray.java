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

package org.mapfish.print.wrapper.multi;

import com.google.common.collect.Lists;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PElement;
import org.mapfish.print.wrapper.PObject;

import java.util.List;


/**
 * Object wrapper for Yaml parsing.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public final class PMultiArray extends PElement implements PArray {
    private final PArray[] arrays;

    /**
     * Build the context name.
     *
     * @param objs the objects
     * @return the global context name
     */
    public static String getContext(final PArray[] objs) {
        StringBuilder result = new StringBuilder("(");
        boolean first = true;
        for (PArray obj : objs) {
            if (!first) {
                result.append('|');
            }
            first = false;
            result.append(obj.getCurrentPath());
        }
        result.append(')');
        return result.toString();
    }

    /**
     * Constructor.
     *
     * @param arrays the possible elements
     */
    public PMultiArray(final PArray[] arrays) {
        super(null, getContext(arrays));
        this.arrays = arrays;
    }

    @Override
    public int size() {
        int maxSize = -1;
        for (PArray array : this.arrays) {
            if (maxSize < array.size()) {
                maxSize = array.size();
            }
        }
        return maxSize;
    }

    @Override
    public PObject getObject(final int i) {
        List<PObject> objs = Lists.newArrayList();
        for (PArray array : this.arrays) {
            if (i < array.size()) {
                objs.add(array.getObject(i));
            }
        }
        if (objs.isEmpty()) {
            throw new ObjectMissingException(this, "" + i);
        }

        if (objs.size() == 1) {
            return objs.get(0);
        }

        return new PMultiObject(objs.toArray(new PObject[objs.size()]));
    }

    @Override
    public PArray getArray(final int i) {
        List<PArray> arr = Lists.newArrayList();
        for (PArray array : this.arrays) {
            if (i < array.size()) {
                arr.add(array.getArray(i));
            }
        }
        if (arr.isEmpty()) {
            throw new ObjectMissingException(this, "" + i);
        }

        if (arr.size() == 1) {
            return arr.get(0);
        }

        return new PMultiArray(arr.toArray(new PArray[arr.size()]));
    }

    @Override
    public Object get(final int i) {
        for (PArray array : this.arrays) {
            if (i < array.size()) {
                return array.get(i);
            }
        }
        throw new ObjectMissingException(this, "" + i);
    }
    @Override
    public int getInt(final int i) {
        return (Integer) get(i);
    }

    @Override
    public float getFloat(final int i) {
        return (Float) get(i);

    }

    @Override
    public double getDouble(final int i) {
        return (Double) get(i);

    }

    @Override
    public String getString(final int i) {
        return (String) get(i);

    }

    @Override
    public boolean getBool(final int i) {
        return (Boolean) get(i);
    }

}
