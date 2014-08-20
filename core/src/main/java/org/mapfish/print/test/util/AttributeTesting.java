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

package org.mapfish.print.test.util;

import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.NorthArrowAttribute;
import org.mapfish.print.attribute.ScalebarAttribute;
import org.mapfish.print.attribute.map.GenericMapAttribute;

/**
 * Support for testing attributes.  This is in main jar because it might be needed across module
 * boundaries and that can be difficult if it is in testing jar.
 *
 * @author Jesse on 8/18/2014.
 * CHECKSTYLE:OFF
 */
public class AttributeTesting {
    /**
     * A few attributes will throw exceptions if not initialized this method can be called when an attribute
     * needs testing but the test is generic and does not necessarily want or need to know the specific
     * type of attribute and its properties.
     */
    public static void configureAttributeForTesting(Attribute att) {
        if (att instanceof GenericMapAttribute) {
            GenericMapAttribute<?> genericMapAttribute = (GenericMapAttribute<?>) att;
            genericMapAttribute.setWidth(500);
            genericMapAttribute.setHeight(500);
            genericMapAttribute.setMaxDpi(400.0);
        } else if (att instanceof ScalebarAttribute) {
            ScalebarAttribute scalebarAttribute = (ScalebarAttribute) att;
            scalebarAttribute.setWidth(300);
            scalebarAttribute.setHeight(120);
        } else if (att instanceof NorthArrowAttribute) {
            NorthArrowAttribute northArrowAttribute = (NorthArrowAttribute) att;
            northArrowAttribute.setSize(50);
        }
    }
}
