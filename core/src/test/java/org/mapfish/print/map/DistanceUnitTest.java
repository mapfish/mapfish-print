package org.mapfish.print.map;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DistanceUnitTest {
    @Test
    public void testConvertTo() {
        final double delta = 0.000001;
        assertEquals(100, DistanceUnit.M.convertTo(1, DistanceUnit.CM), delta);
        assertEquals(1000, DistanceUnit.M.convertTo(1, DistanceUnit.MM), delta);
        assertEquals(10, DistanceUnit.CM.convertTo(1000, DistanceUnit.M), delta);
        assertEquals(10, DistanceUnit.CM.convertTo(1, DistanceUnit.MM), delta);
        assertEquals(1, DistanceUnit.MM.convertTo(10, DistanceUnit.CM), delta);
        assertEquals(2.54, DistanceUnit.IN.convertTo(1, DistanceUnit.CM), delta);
        assertEquals(12, DistanceUnit.FT.convertTo(1, DistanceUnit.IN), delta);
        assertEquals(1, DistanceUnit.IN.convertTo(12, DistanceUnit.FT), delta);
        assertEquals(36, DistanceUnit.YD.convertTo(1, DistanceUnit.IN), delta);
        assertEquals(1, DistanceUnit.IN.convertTo(36, DistanceUnit.YD), delta);
        assertEquals(1, DistanceUnit.FT.convertTo(3, DistanceUnit.YD), delta);
        assertEquals(3, DistanceUnit.YD.convertTo(1, DistanceUnit.FT), delta);
        assertEquals(72.0, DistanceUnit.IN.convertTo(1, DistanceUnit.PX), delta);
        assertEquals(2834.645669, DistanceUnit.M.convertTo(1, DistanceUnit.PX), delta);
        assertEquals(72.0, DistanceUnit.IN.convertTo(1, DistanceUnit.PT), delta);
        assertEquals(1.0, DistanceUnit.PT.convertTo(12, DistanceUnit.PC), delta);
        assertEquals(12.0, DistanceUnit.PC.convertTo(72, DistanceUnit.IN), delta);
        assertEquals(111226, DistanceUnit.DEGREES.convertTo(1, DistanceUnit.M), 1);
        assertEquals(111226, DistanceUnit.MINUTE.convertTo(60, DistanceUnit.M), 1);
        assertEquals(111226, DistanceUnit.SECOND.convertTo(3600, DistanceUnit.M), 1);
        assertEquals(1, DistanceUnit.MINUTE.convertTo(60, DistanceUnit.DEGREES), delta);
        assertEquals(1, DistanceUnit.SECOND.convertTo(60, DistanceUnit.MINUTE), delta);
    }

    @Test
    public void testisSameBaseUnit() {
        assertFalse(DistanceUnit.M.isSameBaseUnit(DistanceUnit.DEGREES));
        assertFalse(DistanceUnit.M.isSameBaseUnit(DistanceUnit.FT));
        assertFalse(DistanceUnit.FT.isSameBaseUnit(DistanceUnit.DEGREES));
        assertFalse(DistanceUnit.FT.isSameBaseUnit(DistanceUnit.M));
        assertFalse(DistanceUnit.DEGREES.isSameBaseUnit(DistanceUnit.M));
        assertFalse(DistanceUnit.DEGREES.isSameBaseUnit(DistanceUnit.FT));

        assertTrue(DistanceUnit.M.isSameBaseUnit(DistanceUnit.CM));
        assertTrue(DistanceUnit.M.isSameBaseUnit(DistanceUnit.MM));
        assertTrue(DistanceUnit.M.isSameBaseUnit(DistanceUnit.KM));
        assertTrue(DistanceUnit.MM.isSameBaseUnit(DistanceUnit.M));
        assertTrue(DistanceUnit.MM.isSameBaseUnit(DistanceUnit.CM));
        assertTrue(DistanceUnit.MM.isSameBaseUnit(DistanceUnit.KM));
        assertTrue(DistanceUnit.CM.isSameBaseUnit(DistanceUnit.M));
        assertTrue(DistanceUnit.CM.isSameBaseUnit(DistanceUnit.MM));
        assertTrue(DistanceUnit.CM.isSameBaseUnit(DistanceUnit.KM));

        assertTrue(DistanceUnit.FT.isSameBaseUnit(DistanceUnit.IN));
        assertTrue(DistanceUnit.FT.isSameBaseUnit(DistanceUnit.YD));
        assertTrue(DistanceUnit.FT.isSameBaseUnit(DistanceUnit.MI));
        assertTrue(DistanceUnit.IN.isSameBaseUnit(DistanceUnit.FT));
        assertTrue(DistanceUnit.IN.isSameBaseUnit(DistanceUnit.YD));
        assertTrue(DistanceUnit.IN.isSameBaseUnit(DistanceUnit.MI));
        assertTrue(DistanceUnit.YD.isSameBaseUnit(DistanceUnit.IN));
        assertTrue(DistanceUnit.YD.isSameBaseUnit(DistanceUnit.FT));
        assertTrue(DistanceUnit.YD.isSameBaseUnit(DistanceUnit.MI));

        assertTrue(DistanceUnit.DEGREES.isSameBaseUnit(DistanceUnit.SECOND));
        assertTrue(DistanceUnit.DEGREES.isSameBaseUnit(DistanceUnit.MINUTE));
        assertTrue(DistanceUnit.MINUTE.isSameBaseUnit(DistanceUnit.SECOND));
        assertTrue(DistanceUnit.MINUTE.isSameBaseUnit(DistanceUnit.DEGREES));
        assertTrue(DistanceUnit.SECOND.isSameBaseUnit(DistanceUnit.MINUTE));
        assertTrue(DistanceUnit.SECOND.isSameBaseUnit(DistanceUnit.SECOND));
    }

    @Test
    public void testGetAllUnits() {
        assertArrayEquals(
                new DistanceUnit[]{DistanceUnit.MM, DistanceUnit.CM, DistanceUnit.M, DistanceUnit.KM},
                DistanceUnit.CM.getAllUnits());
        assertArrayEquals(
                new DistanceUnit[]{DistanceUnit.MM, DistanceUnit.CM, DistanceUnit.M, DistanceUnit.KM},
                DistanceUnit.M.getAllUnits());
        assertArrayEquals(new DistanceUnit[]{
                DistanceUnit.PT,
                DistanceUnit.PC,
                DistanceUnit.IN,
                DistanceUnit.FT,
                DistanceUnit.YD,
                DistanceUnit.MI
        }, DistanceUnit.FT.getAllUnits());
        assertArrayEquals(new DistanceUnit[]{
                DistanceUnit.PT,
                DistanceUnit.PC,
                DistanceUnit.IN,
                DistanceUnit.FT,
                DistanceUnit.YD,
                DistanceUnit.MI
        }, DistanceUnit.MI.getAllUnits());
        assertArrayEquals(
                new DistanceUnit[]{DistanceUnit.SECOND, DistanceUnit.MINUTE, DistanceUnit.DEGREES},
                DistanceUnit.DEGREES.getAllUnits());
        assertArrayEquals(
                new DistanceUnit[]{DistanceUnit.SECOND, DistanceUnit.MINUTE, DistanceUnit.DEGREES},
                DistanceUnit.SECOND.getAllUnits());
    }

    @Test
    public void testGetBestUnit() {
        assertEquals(DistanceUnit.CM, DistanceUnit.getBestUnit(0.01, DistanceUnit.M));
        assertEquals(DistanceUnit.M, DistanceUnit.getBestUnit(0.01, DistanceUnit.KM));
        assertEquals(DistanceUnit.KM, DistanceUnit.getBestUnit(300, DistanceUnit.KM));
    }
}
