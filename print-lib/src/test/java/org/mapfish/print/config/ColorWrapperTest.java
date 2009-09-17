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

import org.mapfish.print.PrintTestCase;

import java.awt.*;

public class ColorWrapperTest extends PrintTestCase {
    public ColorWrapperTest(String name) {
        super(name);
    }

    public void testHexa() {
        ColorWrapper wrapper = new ColorWrapper(Color.class);

        wrapper.setObject("#1256A8");
        assertEquals(new Color(0x12, 0x56, 0xA8), wrapper.getObject());

        wrapper.setObject("#1256b8");
        assertEquals(new Color(0x12, 0x56, 0xb8), wrapper.getObject());
    }

    public void testHexaAlpha() {
        ColorWrapper wrapper = new ColorWrapper(Color.class);

        wrapper.setObject("#1256b823");
        assertEquals(new Color(0x12, 0x56, 0xb8, 0x23), wrapper.getObject());

        wrapper.setObject("#FF56b823");
        assertEquals(new Color(0xFF, 0x56, 0xb8, 0x23), wrapper.getObject());

        wrapper.setObject("#FFFFFFFF");
        assertEquals(new Color(0xFF, 0xFF, 0xFF, 0xFF), wrapper.getObject());
    }

    public void testText() {
        ColorWrapper wrapper = new ColorWrapper(Color.class);

        wrapper.setObject("white");
        assertEquals(Color.white, wrapper.getObject());

        wrapper.setObject("Red");
        assertEquals(Color.red, wrapper.getObject());

        wrapper.setObject("LIGHT_GRAY");
        assertEquals(Color.lightGray, wrapper.getObject());

        wrapper.setObject("BLACK");
        assertEquals(Color.black, wrapper.getObject());

        wrapper.setObject("light gray");
        assertEquals(Color.lightGray, wrapper.getObject());
    }
}
