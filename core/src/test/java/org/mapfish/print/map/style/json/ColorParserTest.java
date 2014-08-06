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

package org.mapfish.print.map.style.json;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;

public class ColorParserTest {

    @Test
    public void testToColor() throws Exception {
        assertEquals(Color.red, ColorParser.toColor("hsla(0, 100%, 0.5f, 1.0)"));
        assertEquals(Color.red, ColorParser.toColor("hsl(0, 1.0f, .5f)"));
        assertEquals(Color.red, ColorParser.toColor("red"));
        assertEquals(Color.red, ColorParser.toColor("red "));
        assertEquals(Color.red, ColorParser.toColor("Red"));
        assertEquals(Color.white, ColorParser.toColor("WHITE"));
        assertEquals(Color.red, ColorParser.toColor("0xff0000"));
        assertEquals(Color.red, ColorParser.toColor("#F00"));
        assertEquals(Color.red, ColorParser.toColor("#FF0000"));
        assertEquals(Color.red, ColorParser.toColor("#FF0000 "));
        assertEquals(Color.red, ColorParser.toColor("rgb(255, 0, 0)"));
        assertEquals(Color.red, ColorParser.toColor("rgb(255, 0, 0) "));
        assertEquals(Color.gray, ColorParser.toColor("rgb(128, 128, 128) "));
        assertEquals(Color.red, ColorParser.toColor("rgb(100%, 0%, 0%)"));
        assertEquals(Color.red, ColorParser.toColor("rgb(100%, 0%, 0%) "));
        assertEquals(new Color(1.0f, 0.0f, 0.0f, 0.5f), ColorParser.toColor("rgba(255, 0, 0, 0.5)"));
    }

}