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

package org.mapfish.print.processor.map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.map.Scale;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MapfishMapContextTest {

    @Test
    public void testGetRoundedScale() {
        assertRoundedScale(0.111, 0.111);
        assertRoundedScale(0.11, 0.11);
        assertRoundedScale(9, 9);
        assertRoundedScale(9.1, 9);
        assertRoundedScale(239, 240);
        assertRoundedScale(993, 990);
        assertRoundedScale(1113, 1100);
        assertRoundedScale(11130, 11000);
        assertRoundedScale(225229, 230000);
        assertRoundedScale(1234229, 1200000);
        assertRoundedScale(12342290, 12000000);
    }

    private void assertRoundedScale(double actualScale, double roundedScale) {
        MapBounds bounds = new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 0, 0, new Scale(actualScale));
        Dimension paint = new Dimension(200, 200);
        MapfishMapContext transformer = new MapfishMapContext(bounds, paint, 90, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        assertEquals(roundedScale, transformer.getRoundedScale(), 0.00001);
    }

    @Test
    public void testGetRotation() {
        MapfishMapContext transformer = new MapfishMapContext(null, null, 90, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        assertEquals("converted to radians", Math.toRadians(90.0), transformer.getRotation(), 1e-9);
    }

    @Test
    public void testGetRotatedBounds_BBoxMapBounds() {
        MapBounds bounds = new BBoxMapBounds(DefaultGeographicCRS.WGS84, 5, 45, 25, 55);
        
        // rotate 90 degree
        MapfishMapContext transformer = new MapfishMapContext(bounds, null, 90, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        MapBounds rotatedBounds = new BBoxMapBounds(DefaultGeographicCRS.WGS84, 10, 40, 20, 60);
        assertEquals(rotatedBounds, transformer.getRotatedBounds());

        // rotate 180 degree
        transformer = new MapfishMapContext(bounds, null, 180, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        rotatedBounds = new BBoxMapBounds(DefaultGeographicCRS.WGS84, 5, 45, 25, 55);
        assertEquals(rotatedBounds, transformer.getRotatedBounds());

        // rotate 45 degree
        transformer = new MapfishMapContext(bounds, null, 45, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        ReferencedEnvelope rotatedEnvelope = 
                transformer.getRotatedBounds().toReferencedEnvelope(new Rectangle(1, 1), 72);
        assertEquals(4.393398, rotatedEnvelope.getMinX(), 1e-6);
        assertEquals(25.606601, rotatedEnvelope.getMaxX(), 1e-6);
        assertEquals(39.393398, rotatedEnvelope.getMinY(), 1e-6);
        assertEquals(60.606601, rotatedEnvelope.getMaxY(), 1e-6);

        // rotate 45 degree
        bounds = new BBoxMapBounds(DefaultGeographicCRS.WGS84, -0.5, -0.5, 0.5, 0.5);
        transformer = new MapfishMapContext(bounds, null, 45, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        rotatedEnvelope = 
                transformer.getRotatedBounds().toReferencedEnvelope(new Rectangle(1, 1), 72);
        assertEquals(-0.707106, rotatedEnvelope.getMinX(), 1e-6);
        assertEquals(0.707106, rotatedEnvelope.getMaxX(), 1e-6);
        assertEquals(-0.707106, rotatedEnvelope.getMinY(), 1e-6);
        assertEquals(0.707106, rotatedEnvelope.getMaxY(), 1e-6);
    }

    @Test
    public void testGetRotatedBounds_CenterScaleMapBounds() {
        MapBounds bounds = new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 0, 0, new Scale(1000));
        MapfishMapContext transformer = new MapfishMapContext(bounds, null, 90, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        // nothing changes
        assertEquals(bounds, transformer.getRotatedBounds());
    }

    @Test
    public void testGetRotatedMapSize() {
        // no rotation
        MapfishMapContext transformer = new MapfishMapContext(null, new Dimension(1, 1), 0, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        assertEquals(new Dimension(1, 1), transformer.getRotatedMapSize());

        // rotate 90 degree
        transformer = new MapfishMapContext(null, new Dimension(2, 1), 90, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        assertEquals(new Dimension(1, 2), transformer.getRotatedMapSize());

        // rotate 180 degree
        transformer = new MapfishMapContext(null, new Dimension(2, 1), 180, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        assertEquals(new Dimension(2, 1), transformer.getRotatedMapSize());

        // rotate 45 degree
        transformer = new MapfishMapContext(null, new Dimension(100, 100), 45, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        Dimension rotatedMapSize = transformer.getRotatedMapSize();
        assertEquals(141, rotatedMapSize.getWidth(), 1e-6);
        assertEquals(141, rotatedMapSize.getHeight(), 1e-6);
    }

    @Test
    public void testGetTransform() {
        MapBounds bounds = new BBoxMapBounds(DefaultGeographicCRS.WGS84, -0.5, -0.5, 0.5, 0.5);
        Dimension mapSize = new Dimension(100, 100);
        
        // no rotation
        MapfishMapContext transformer = new MapfishMapContext(bounds, mapSize, 0, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        assertNull(transformer.getTransform());
        
        // rotate 180 degree
        transformer = new MapfishMapContext(bounds, mapSize, 180, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        AffineTransform transform = transformer.getTransform();
        assertEquals(100, transform.getTranslateX(), 1e-6);
        assertEquals(100, transform.getTranslateY(), 1e-6);
        
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        assertArrayEquals(new double[] {-1.0, 0.0, 0.0, -1.0, 100.0, 100.0}, matrix, 1e-6);
        
        // rotate 90 degree
        transformer = new MapfishMapContext(bounds, mapSize, 90, Constants.PDF_DPI, Constants.PDF_DPI, null, true);
        transform = transformer.getTransform();
        assertEquals(100, transform.getTranslateX(), 1e-6);
        assertEquals(0, transform.getTranslateY(), 1e-6);
        
        matrix = new double[6];
        transform.getMatrix(matrix);
        assertArrayEquals(new double[] {0.0, 1.0, -1.0, -0.0, 100.0, 0.0}, matrix, 1e-6);
    }

}
