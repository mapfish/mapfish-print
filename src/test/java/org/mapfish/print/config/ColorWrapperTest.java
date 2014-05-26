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

import static org.junit.Assert.*;

import com.itextpdf.text.BaseColor;

import org.junit.Test;

import org.mapfish.print.PrintTestCase;

public class ColorWrapperTest extends PrintTestCase {

    @Test
    public void testHexa() {
        ColorWrapper wrapper = new ColorWrapper(BaseColor.class);

        wrapper.setObject("#1256A8");
        assertEquals(new BaseColor(0x12, 0x56, 0xA8), wrapper.getObject());

        wrapper.setObject("#1256b8");
        assertEquals(new BaseColor(0x12, 0x56, 0xb8), wrapper.getObject());
    }

    @Test
    public void testHexaAlpha() {
        ColorWrapper wrapper = new ColorWrapper(BaseColor.class);

        wrapper.setObject("#1256b8");
        assertEquals(new BaseColor(0x12, 0x56, 0xb8), wrapper.getObject());

        wrapper.setObject("#FF56b8");
        assertEquals(new BaseColor(0xFF, 0x56, 0xb8), wrapper.getObject());

        wrapper.setObject("#FFF");
        assertEquals(new BaseColor(0xFF, 0xFF, 0xFF), wrapper.getObject());
    }

    @Test
    public void testText() {
        ColorWrapper wrapper = new ColorWrapper(BaseColor.class);

        wrapper.setObject("white");
        assertEquals(BaseColor.WHITE, wrapper.getObject());

        wrapper.setObject("Red");
        assertEquals(BaseColor.RED, wrapper.getObject());

        wrapper.setObject("Silver");
        assertEquals(BaseColor.LIGHT_GRAY, wrapper.getObject());

        wrapper.setObject("BLACK");
        assertEquals(BaseColor.BLACK, wrapper.getObject());

        wrapper.setObject("yellow");
        assertEquals(BaseColor.YELLOW, wrapper.getObject());
    }
}
