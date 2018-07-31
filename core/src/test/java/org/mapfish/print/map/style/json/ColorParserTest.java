package org.mapfish.print.map.style.json;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.assertEquals;

public class ColorParserTest {

    @Test
    public void testToColor() {
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

    public void testToRGB() {
        assertEquals("rgb(1, 2, 3)", ColorParser.toRGB(ColorParser.toColor("rgb(1, 2, 3)")));
    }
}
