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

package org.mapfish.print.config;

import org.ho.yaml.exception.YamlException;
import org.ho.yaml.wrapper.AbstractWrapper;
import org.ho.yaml.wrapper.SimpleObjectWrapper;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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


    public static Color convertColor(String color) {
        if (color == null) return null;

        //look for a system ressource named like that
        Color result = Color.getColor(color);

        //try to decode stuff like #FFFFFF
        if (result == null) {
            try {
                Long longval = Long.decode(color);
                long i = longval.longValue();
                if (i >= 0x1000000L) {
                    result = new Color((int) (i >> 24) & 0xFF, (int) (i >> 16) & 0xFF, (int) (i >> 8) & 0xFF, (int) i & 0xFF);
                } else {
                    result = new Color((int) (i >> 16) & 0xFF, (int) (i >> 8) & 0xFF, (int) i & 0xFF);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        //look for a constant in the Color class with the given name
        if (result == null) {
            try {
                final Field field = Color.class.getField(color.toUpperCase().replaceAll(" ", "_"));
                if (field != null && Modifier.isStatic(field.getModifiers()) &&
                        Modifier.isPublic(field.getModifiers())) {
                    result = (Color) field.get(Color.class);
                }
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException ignored) {
            }
        }
        if (result == null) {
            throw new YamlException("Invalid color: " + color);
        }
        return result;
    }
}