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

package org.mapfish.print.utils;

import org.mapfish.print.PrintTestCase;

import java.nio.charset.Charset;

public class DistanceUnitTest extends PrintTestCase {
    public DistanceUnitTest(String name) {
        super(name);
    }

    public void testString() {
        assertEquals("m", DistanceUnit.fromString("Meter").toString());
        assertEquals("m", DistanceUnit.fromString("meter").toString());
        assertEquals("m", DistanceUnit.fromString("meterS").toString());
        assertEquals("m", DistanceUnit.fromString("m").toString());
        assertEquals("\u00B0", DistanceUnit.fromString("degree").toString());
    }

    public void testConvert() {
        assertEquals(25.4 / 1000.0, DistanceUnit.IN.convertTo(1.0, DistanceUnit.M));
        assertEquals(25.4, DistanceUnit.IN.convertTo(1.0, DistanceUnit.MM));
        assertEquals(1000.0, DistanceUnit.M.convertTo(1.0, DistanceUnit.MM));
        assertEquals(1 / 12.0, DistanceUnit.IN.convertTo(1.0, DistanceUnit.FT));
        assertEquals(12.0, DistanceUnit.FT.convertTo(1.0, DistanceUnit.IN));
    }

    public void testGroup() {
        DistanceUnit[] metrics = DistanceUnit.MM.getAllUnits();
        assertEquals(4, metrics.length);
        assertSame(DistanceUnit.MM, metrics[0]);
        assertSame(DistanceUnit.CM, metrics[1]);
        assertSame(DistanceUnit.M, metrics[2]);
        assertSame(DistanceUnit.KM, metrics[3]);
    }

    public void testBestUnit() {
        assertEquals(DistanceUnit.M, DistanceUnit.getBestUnit(1000.0, DistanceUnit.MM));
        assertEquals(DistanceUnit.CM, DistanceUnit.getBestUnit(999.9, DistanceUnit.MM));
        assertEquals(DistanceUnit.KM, DistanceUnit.getBestUnit(1e12, DistanceUnit.M));
        assertEquals(DistanceUnit.MM, DistanceUnit.getBestUnit(1e-12, DistanceUnit.M));
    }
}
