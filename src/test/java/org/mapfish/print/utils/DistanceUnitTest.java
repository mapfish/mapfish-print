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

package org.mapfish.print.utils;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mapfish.print.PrintTestCase;

public class DistanceUnitTest extends PrintTestCase {

    @Test
    public void testString() {
        assertEquals("m", DistanceUnit.fromString("Meter").toString());
        assertEquals("m", DistanceUnit.fromString("meter").toString());
        assertEquals("m", DistanceUnit.fromString("meterS").toString());
        assertEquals("m", DistanceUnit.fromString("m").toString());
        assertEquals("\u00B0", DistanceUnit.fromString("degree").toString());
    }

    @Test
    public void testConvert() {
        assertEquals(25.4 / 1000.0, DistanceUnit.IN.convertTo(1.0, DistanceUnit.M), 0.000001);
        assertEquals(25.4, DistanceUnit.IN.convertTo(1.0, DistanceUnit.MM), 0.000001);
        assertEquals(1000.0, DistanceUnit.M.convertTo(1.0, DistanceUnit.MM), 0.000001);
        assertEquals(1 / 12.0, DistanceUnit.IN.convertTo(1.0, DistanceUnit.FT), 0.000001);
        assertEquals(12.0, DistanceUnit.FT.convertTo(1.0, DistanceUnit.IN), 0.000001);
    }

    @Test
    public void testGroup() {
        DistanceUnit[] metrics = DistanceUnit.MM.getAllUnits();
        assertEquals(4, metrics.length);
        assertSame(DistanceUnit.MM, metrics[0]);
        assertSame(DistanceUnit.CM, metrics[1]);
        assertSame(DistanceUnit.M, metrics[2]);
        assertSame(DistanceUnit.KM, metrics[3]);
    }

    @Test
    public void testBestUnit() {
        assertEquals(DistanceUnit.M, DistanceUnit.getBestUnit(1000.0, DistanceUnit.MM));
        assertEquals(DistanceUnit.CM, DistanceUnit.getBestUnit(999.9, DistanceUnit.MM));
        assertEquals(DistanceUnit.KM, DistanceUnit.getBestUnit(1e12, DistanceUnit.M));
        assertEquals(DistanceUnit.MM, DistanceUnit.getBestUnit(1e-12, DistanceUnit.M));
    }
}
