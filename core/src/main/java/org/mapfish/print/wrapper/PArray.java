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
 * Array wrapper interface for Json and Yaml parsing.
 *
 * @author Stéphane Brunner on 11/04/14.
 */
public interface PArray {
    /**
     * Return the size of the array.
     */
    int size();

    /**
     * Get the element at the index as a object.
     * @param i the index of the object to access
     */
    PObject getObject(final int i);

    /**
     * Get the element at the index as a json array.
     * @param i the index of the element to access
     */
    PArray getArray(final int i);

    /**
     * Get the element at the index as an integer.
     * @param i the index of the element to access
     */
    int getInt(final int i);

    /**
     * Get the element at the index as a float.
     * @param i the index of the element to access
     */
    float getFloat(final int i);

    /**
     * Get the element at the index as a double.
     * @param i the index of the element to access
     */
    double getDouble(final int i);

    /**
     * Get the element at the index as a string.
     * @param i the index of the element to access
     */
    String getString(final int i);

    /**
     * Get the element as a boolean.
     * @param i the index of the element to access
     */
    boolean getBool(final int i);

    /**
     * Gets the string representation of the path to the current element.
     *
     * @param key the leaf key
     */
    String getPath(final String key);

    /**
     * Gets the string representation of the path to the current element.
     */
    String getCurrentPath();

    /**
     * Get the object at the given index.
     *
     * @param i the index of the element to access
     */
    Object get(int i);
}
