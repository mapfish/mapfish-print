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

import org.mapfish.print.wrapper.PAbstractObject;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Object wrapper for Yaml parsing.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public class PMultiObject extends PAbstractObject {
    private final PObject[] objs;

    /**
     * Build the context name.
     *
     * @param objs the objects
     * @return the global context name
     */
    public static String getContext(final PObject[] objs) {
        StringBuilder result = new StringBuilder("(");
        boolean first = true;
        for (PObject obj : objs) {
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
     * @param objs the possible elements
     */
    public PMultiObject(final PObject[] objs) {
        super(null, getContext(objs));
        this.objs = objs;
    }

    @Override
    public final Object opt(final String key) {
        for (PObject obj : this.objs) {
            Object result = obj.opt(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public final String optString(final String key) {
        for (PObject obj : this.objs) {
            String result = obj.optString(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public final Integer optInt(final String key) {
        for (PObject obj : this.objs) {
            Integer result = obj.optInt(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public final Double optDouble(final String key) {
        for (PObject obj : this.objs) {
            Double result = obj.optDouble(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public final Float optFloat(final String key) {
        for (PObject obj : this.objs) {
            Float result = obj.optFloat(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public final Boolean optBool(final String key) {
        for (PObject obj : this.objs) {
            Boolean result = obj.optBool(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public final PObject optObject(final String key) {
        List<PObject> results = new ArrayList<PObject>();
        for (PObject obj : this.objs) {
            PObject result = obj.optObject(key);
            if (result != null) {
                results.add(result);
            }
        }
        if (results.size() == 0) {
            return null;
        }
        if (results.size() == 1) {
            return results.get(0);
        }
        return new PMultiObject(results.toArray(new PObject[results.size()]));
    }

    @Override
    public final PArray optArray(final String key) {
        for (PObject obj : this.objs) {
            PArray result = obj.optArray(key);
            if (result != null) {
                return result;
            }
        }
            return null;
    }

    @Override
    public final boolean isArray(final String key) {
        for (PObject obj : this.objs) {
            if (obj.has(key)) {
                return obj.isArray(key);
            }
        }
        return false;
    }

    private Set<String> allKeys() {
        Set<String> keys = new HashSet<String>();
        for (PObject obj : this.objs) {
            Iterator<String> customParamsIter = obj.keys();
            while (customParamsIter.hasNext()) {
                keys.add(customParamsIter.next());
            }
        }
        return keys;
    }

    @Override
    public final Iterator<String> keys() {
        return allKeys().iterator();
    }

    @Override
    public final int size() {
        return allKeys().size();
    }

    @Override
    public final boolean has(final String key) {
        for (PObject obj : this.objs) {
            if (obj.has(key)) {
                return true;
            }
        }
        return false;
    }
}
