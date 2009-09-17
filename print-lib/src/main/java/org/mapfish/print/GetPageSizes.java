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

import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Non production utility main to print the pages sizes defined by iText.
 */
public class GetPageSizes {
    public static void main(String[] args) throws IllegalAccessException {
        Field[] fields = PageSize.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    Rectangle val = (Rectangle) field.get(null);
                    System.out.println(field.getName() + ": " + Math.round(val.getWidth()) + "x" + Math.round(val.getHeight()));
                } catch (Throwable ex) {
                    System.out.println("Error with: " + field.getModifiers());
                }
            }
        }
    }
}
