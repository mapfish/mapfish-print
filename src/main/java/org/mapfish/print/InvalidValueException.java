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

package org.mapfish.print;

/**
 * Thrown when there is something invalid in the YAML file
 */
public class InvalidValueException extends PrintException {
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
