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

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.mapfish.print.config.Config;
import org.mapfish.print.utils.DistanceUnit;
import org.mockito.Mockito;

public class TransformerTest extends PrintTestCase {

    @Test
    public void testStraight() {
        Transformer t = new Transformer(0, 0, 100, 70, 10, 2, DistanceUnit.fromString("m"), 0, null, false, false);
        assertEquals(100.0F, t.getPaperW(), 0.00001);
        assertEquals(70.0F, t.getPaperH(), 0.00001);

        final double geoW = 100.0F / 72.0F * 2.54 / 10.0F;
        final double geoH = 70.0F / 72.0F * 2.54 / 10.0F;
        assertEquals(geoW, t.getGeoW(), .000001);
        assertEquals(geoH, t.getGeoH(), .000001);
        assertEquals(geoW, t.getRotatedGeoW(), .000001);
        assertEquals(geoH, t.getRotatedGeoH(), .000001);
    }


    @Test
    public void testGeodetic() {
        DistanceUnit unitEnum = DistanceUnit.DEGREES;
        int dpi = 2;
        int scale = 100000;
        Transformer geodetic = new Transformer(0, 0, 100, 70, scale, dpi, unitEnum, 0, "EPSG:4326", false, true);
        Transformer linear = new Transformer(0, 0, 100, 70, scale, dpi, unitEnum, 0, null, false, false);
        assertEquals(linear.getPaperW(), geodetic.getPaperW(), 0.00001);
        assertEquals(linear.getPaperH(), geodetic.getPaperH(), 0.00001);

        assertTrue(Math.abs(linear.getGeoH() - geodetic.getGeoH()) > 0.00000001);
        assertTrue(Math.abs(linear.getMaxGeoX() - geodetic.getMaxGeoX()) > 0.00000001);
        assertTrue(Math.abs(linear.getMinGeoX() - geodetic.getMinGeoX()) > 0.00000001);
        assertTrue(Math.abs(linear.getMaxGeoY() - geodetic.getMaxGeoY()) > 0.00000001);
        assertTrue(Math.abs(linear.getMinGeoY() - geodetic.getMinGeoY()) > 0.00000001);
    }
    @Test
    public void testGoogle() {
        DistanceUnit unitEnum = DistanceUnit.fromString("m");
		int dpi = 2;
		int scale = 10;
        Transformer geodetic = new Transformer(731033.0f,5864001.0f, 100, 70, scale, dpi, unitEnum, 0, "EPSG:900913", false, false);
        Transformer linear = new Transformer(6.566981170957462f, 46.51954387957121f, 100, 70, scale, dpi, unitEnum, 0, null, false, false);
        assertEquals(linear.getPaperW(), geodetic.getPaperW(), 0.00001);
        assertEquals(linear.getPaperH(), geodetic.getPaperH(), 0.00001);

        assertTrue(Math.abs(linear.getGeoH() - geodetic.getGeoH()) > 0.00000001);
    }
//
//    @Test
//    public void testGetStraightBitmapW() throws Exception {
//        double minGeoX = 589434.4971235897;
//        double minGeoY = 4913947.342298816;
//        double maxGeoX = 609518.2117427464;
//        double maxGeoY = 4928071.049965891;
//
//        Config configObj = new Config();
//        configObj.setDisableScaleLocking(true);
//        final int paperWidth = 512;
//        final int paperHeight = 360;
//
//        final Transformer transformer = new Transformer(minGeoX, minGeoY, maxGeoX, maxGeoY, paperWidth, paperHeight, 72, DistanceUnit.M, 0, false,
//                configObj, false);
//
//
//        assertEquals(paperWidth, transformer.getStraightBitmapW(), 0.0000001);
//        assertEquals(paperHeight, transformer.getStraightBitmapW(), 0.0000001);
//
//    }
}
