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

package org.mapfish.print.json;

import org.mapfish.print.PrintException;

/**
 * Thrown when an attribute has an invalid value in the spec.
 */
public class InvalidJsonValueException extends PrintException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param element element that was queried
     * @param key key that was desired
     * @param value the illegal value obtained.
     */
    public InvalidJsonValueException(final PJsonElement element, final String key, final Object value) {
        this(element, key, value, null);
    }

    /**
     * Constructor.
     *
     * @param element element that was queried
     * @param key key that was desired
     * @param value the illegal value obtained.
     * @param e the exception to wrap by this exception
     */
    public InvalidJsonValueException(final PJsonElement element, final String key, final Object value, final Throwable e) {
        super(element.getPath(key) + " has an invalid value: " + value.toString(), e);
    }

    @Override
	public final String toString() {
        if (getCause() != null) {
            return super.toString() + " (" + getCause().getMessage() + ")";
        } else {
            return super.toString();
        }
    }
}
