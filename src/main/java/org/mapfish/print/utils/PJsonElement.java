/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.utils;

/**
 * Common parent class for the JSON wrappers.
 */
public abstract class PJsonElement {
    private final PJsonElement parent;
    private final String contextName;

    protected PJsonElement(PJsonElement parent, String contextName) {
        this.parent = parent;
        this.contextName = contextName;
    }

    /**
     * Gets the string representation of the path to the current JSON element.
     */
    public String getPath(String key) {
        StringBuilder result = new StringBuilder();
        getPath(result);
        result.append(".");
        result.append(getPathElement(key));
        return result.toString();
    }

    protected void getPath(StringBuilder result) {
        if (parent != null) {
            parent.getPath(result);
            if (!(parent instanceof PJsonArray)) {
                result.append(".");
            }
        }
        result.append(getPathElement(contextName));
    }

    private static String getPathElement(String val) {
        if (val.contains(" ")) {
            return "'" + val + "'";
        } else {
            return val;
        }
    }


    public PJsonElement getParent() {
        return parent;
    }
}
