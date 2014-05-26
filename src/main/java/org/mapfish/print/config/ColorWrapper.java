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

package org.mapfish.print.config;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.html.WebColors;

import org.ho.yaml.wrapper.AbstractWrapper;
import org.ho.yaml.wrapper.SimpleObjectWrapper;

/**
 * Yaml wrapper for allowing color fields. The supported formats are:
 * <ul>
 * <li> hexadecimal, like: #FFFFFF
 * <li> strings like (in fact all the constants declared in the Color class): white, black, red, ...
 * </ul>
 */
public class ColorWrapper extends AbstractWrapper implements SimpleObjectWrapper {
    public ColorWrapper(Class<?> type) {
        super(type);
    }

    public void setObject(Object obj) {
        if (obj instanceof String) {
            super.setObject(convertColor((String) obj));
        } else {
            super.setObject(obj);
        }
    }

    public Class<?> expectedArgType() {
        return String.class;
    }

    public Object getOutputValue() {
        return getObject().toString();
    }

    public static BaseColor convertColor(String color) throws IllegalArgumentException {
        
        if (color == null) {
            return null;
        } else {
            return WebColors.getRGBColor(color);
        }

    }
}