/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print;

/**
 * Thrown when there is something invalid in the YAML file
 */
public class InvalidValueException extends PrintException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public InvalidValueException(String name, String value) {
        this(name, value, null);
    }

    public InvalidValueException(String name, String value, Throwable e) {
        super(name + " has an invalid value: " + value, e);
    }

    public InvalidValueException(String name, int value) {
        this(name, Integer.toString(value));
    }

    public InvalidValueException(String name, double value) {
        this(name, Double.toString(value));
    }
}
