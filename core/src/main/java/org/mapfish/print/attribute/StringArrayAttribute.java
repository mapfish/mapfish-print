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

package org.mapfish.print.attribute;

/**
 * An attribute that can contain an array of strings.
 * @author Jesse on 9/13/2014.
 */
public class StringArrayAttribute extends PrimitiveAttribute<String[]> {
    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    public StringArrayAttribute() {
        super((Class<String[]>) new String[0].getClass());
    }
}
