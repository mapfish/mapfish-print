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

import org.mapfish.print.wrapper.json.PJsonArray;

/**
 * Common parent class for the Json and Yaml wrappers.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public abstract class PElement {
    private final PElement parent;
    private final String contextName;

    /**
     * Constructor.
     *
     * @param parent the parent element
     * @param contextName the field name of this element in the parent.
     */
    protected PElement(final PElement parent, final String contextName) {
        this.parent = parent;
        this.contextName = contextName;
    }

    /**
     * Gets the string representation of the path to the current JSON element.
     *
     * @param key the leaf key
     */
    public final String getPath(final String key) {
        StringBuilder result = new StringBuilder();
        addPathTo(result);
        result.append(".");
        result.append(getPathElement(key));
        return result.toString();
    }

    protected final String getContextName() {
        return this.contextName;
    }

    /**
     * Gets the string representation of the path to the current JSON element.
     */
    public final String getCurrentPath() {
        StringBuilder result = new StringBuilder();
        addPathTo(result);
        return result.toString();
    }

    /**
     * Append the path to the StringBuilder.
     * @param result the string builder to add the path to.
     */
    protected final void addPathTo(final StringBuilder result) {
        if (this.parent != null) {
            this.parent.addPathTo(result);
            if (!(this.parent instanceof PJsonArray)) {
                result.append(".");
            }
        }
        result.append(getPathElement(this.contextName));
    }

    private static String getPathElement(final String val) {
        if (val == null) {
            return "";
        }
        if (val.contains(" ")) {
            return "'" + val + "'";
        } else {
            return val;
        }
    }



    public final PElement getParent() {
        return this.parent;
    }
}
