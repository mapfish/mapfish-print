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

package org.mapfish.print;

import org.mapfish.print.utils.DistanceUnit;

public class TransformerTest extends PrintTestCase {
    public TransformerTest(String name) {
        super(name);
    }

    public void testStraight() {
        Transformer t = new Transformer(0, 0, 100, 70, 10, 2, DistanceUnit.fromString("m"), 0, null, false);
        assertEquals(100.0F, t.getPaperW());
        assertEquals(70.0F, t.getPaperH());

        final double geoW = 100.0F / 72.0F * 2.54 / 10.0F;
        final double geoH = 70.0F / 72.0F * 2.54 / 10.0F;
        assertEquals(geoW, t.getGeoW(), .000001);
        assertEquals(geoH, t.getGeoH(), .000001);
        assertEquals(geoW, t.getRotatedGeoW(), .000001);
        assertEquals(geoH, t.getRotatedGeoH(), .000001);
    }

    public void testGeodetic() {
        DistanceUnit unitEnum = DistanceUnit.fromString("m");
		int dpi = 2;
		int scale = 10;
        Transformer geodetic = new Transformer(0, 0, 100, 70, scale, dpi, unitEnum, 0, "EPSG:4326", false);
        Transformer linear = new Transformer(0, 0, 100, 70, scale, dpi, unitEnum, 0, null, false);
        assertEquals(linear.getPaperW(), geodetic.getPaperW());
        assertEquals(linear.getPaperH(), geodetic.getPaperH());

        assertTrue(Math.abs(linear.getGeoH() - geodetic.getGeoH()) > 0.00000001);
    }
    

    public void testGoogle() {
        DistanceUnit unitEnum = DistanceUnit.fromString("m");
		int dpi = 2;
		int scale = 10;
        Transformer geodetic = new Transformer(731033.0f,5864001.0f, 100, 70, scale, dpi, unitEnum, 0, "EPSG:900913", false);
        Transformer linear = new Transformer(6.566981170957462f, 46.51954387957121f, 100, 70, scale, dpi, unitEnum, 0, null, false);
        assertEquals(linear.getPaperW(), geodetic.getPaperW());
        assertEquals(linear.getPaperH(), geodetic.getPaperH());

        assertTrue(Math.abs(linear.getGeoH() - geodetic.getGeoH()) > 0.00000001);
        
    }

}
