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

package org.mapfish.print.attribute.map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;
import static org.mapfish.print.attribute.map.CenterScaleMapBoundsTest.CH1903;

/**
 * @author Jesse on 3/27/14.
 */
public class BBoxMapBoundsTest {
    public static final CoordinateReferenceSystem SPHERICAL_MERCATOR;

    static {
        try {
            SPHERICAL_MERCATOR = CRS.decode("EPSG:3857");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testToReferencedEnvelope() throws Exception {
        final BBoxMapBounds bboxMapBounds = new BBoxMapBounds(WGS84, -180, -90, 180, 90);

        final ReferencedEnvelope envelope = bboxMapBounds.toReferencedEnvelope(new Rectangle(10, 5), 90);


        assertEquals(-180, envelope.getMinX(), 0.001);
        assertEquals(180, envelope.getMaxX(), 0.001);
        assertEquals(-90, envelope.getMinY(), 0.001);
        assertEquals(90, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testAdjustedEnvelope() throws Exception {
        final BBoxMapBounds bboxMapBounds = new BBoxMapBounds(WGS84, -10, -90, 10, 90);
        final MapBounds mapBounds = bboxMapBounds.adjustedEnvelope(new Rectangle(5, 5));
        final ReferencedEnvelope envelope = mapBounds.toReferencedEnvelope(new Rectangle(5, 5), 90);

        assertEquals(-90, envelope.getMinX(), 0.001);
        assertEquals(90, envelope.getMaxX(), 0.001);
        assertEquals(-90, envelope.getMinY(), 0.001);
        assertEquals(90, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testAdjustToScale() throws Exception {
        int scale = 24000;
        double dpi = 100;
        Rectangle screen = new Rectangle(100, 100);
        ZoomLevels zoomLevels = new ZoomLevels(15000, 20000, 25000, 30000, 350000);


        final CenterScaleMapBounds mapBounds = new CenterScaleMapBounds(CH1903, 50000, 50000, new Scale(scale));
        final ReferencedEnvelope originalBBox = mapBounds.toReferencedEnvelope(screen, dpi);

        BBoxMapBounds linear = new BBoxMapBounds(CH1903, originalBBox.getMinX(), originalBBox.getMinY(),
                originalBBox.getMaxX(), originalBBox.getMaxY());

        final MapBounds newMapBounds = linear.adjustBoundsToNearestScale(zoomLevels, 0.05,
                ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE, false, screen, dpi);
        ReferencedEnvelope newBBox = newMapBounds.toReferencedEnvelope(screen, dpi);

        final double delta = 0.00001;
        assertEquals(originalBBox.getMedian(0), newBBox.getMedian(0), delta);
        assertEquals(originalBBox.getMedian(1), newBBox.getMedian(1), delta);

        double expectedScale = 25000;
        CenterScaleMapBounds expectedMapBounds = new CenterScaleMapBounds(CH1903, originalBBox.centre().x, originalBBox.centre().y,
                new Scale(expectedScale));
        assertEquals(expectedMapBounds.toReferencedEnvelope(screen, dpi), newBBox);
    }

    @Test
    public void testAdjustToScaleLatLong() throws Exception {
        int scale = 24000;
        double dpi = 100;
        Rectangle screen = new Rectangle(100, 100);
        ZoomLevels zoomLevels = new ZoomLevels(15000, 20000, 25000, 30000, 350000);


        final CenterScaleMapBounds mapBounds = new CenterScaleMapBounds(WGS84, 5, 5, new Scale(scale));
        final ReferencedEnvelope originalBBox = mapBounds.toReferencedEnvelope(screen, dpi);

        BBoxMapBounds linear = new BBoxMapBounds(WGS84, originalBBox.getMinX(), originalBBox.getMinY(),
                originalBBox.getMaxX(), originalBBox.getMaxY());

        final MapBounds newMapBounds = linear.adjustBoundsToNearestScale(zoomLevels, 0.05,
                ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE, false,
                screen, dpi);
        ReferencedEnvelope newBBox = newMapBounds.toReferencedEnvelope(screen, dpi);

        final double delta = 0.00001;
        assertEquals(originalBBox.getMedian(0), newBBox.getMedian(0), delta);
        assertEquals(originalBBox.getMedian(1), newBBox.getMedian(1), delta);

        double expectedScale = 25000;
        CenterScaleMapBounds expectedMapBounds = new CenterScaleMapBounds(WGS84, originalBBox.centre().x, originalBBox.centre().y,
                new Scale(expectedScale));
        assertEquals(expectedMapBounds.toReferencedEnvelope(screen, dpi), newBBox);
    }

    @Test
    public void testZoomOut() throws Exception {
        BBoxMapBounds bboxMapBounds = new BBoxMapBounds(WGS84, -10, -10, 10, 10);
        MapBounds bounds = bboxMapBounds.zoomOut(1);
        assertEquals(bboxMapBounds, bounds);

        bounds = bboxMapBounds.zoomOut(2);
        ReferencedEnvelope envelope = bounds.toReferencedEnvelope(new Rectangle(5, 5), 90);

        assertEquals(-20, envelope.getMinX(), 0.001);
        assertEquals(20, envelope.getMaxX(), 0.001);
        assertEquals(-20, envelope.getMinY(), 0.001);
        assertEquals(20, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());

        bboxMapBounds = new BBoxMapBounds(WGS84, -10, -5, 10, 5);
        bounds = bboxMapBounds.zoomOut(1);
        assertEquals(bboxMapBounds, bounds);

        bounds = bboxMapBounds.zoomOut(4);
        envelope = bounds.toReferencedEnvelope(new Rectangle(40, 20), 90);

        assertEquals(-40, envelope.getMinX(), 0.001);
        assertEquals(40, envelope.getMaxX(), 0.001);
        assertEquals(-20, envelope.getMinY(), 0.001);
        assertEquals(20, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testAdjustToGeodeticScale() throws Exception {
        int scale = 24000;
        double dpi = 100;
        Rectangle screen = new Rectangle(100, 100);
        ZoomLevels zoomLevels = new ZoomLevels(15000, 20000, 25000, 30000, 350000);

        final CenterScaleMapBounds mapBounds = new CenterScaleMapBounds(SPHERICAL_MERCATOR, 400000, 5000000, new Scale(scale));
        final ReferencedEnvelope originalBBox = mapBounds.toReferencedEnvelope(screen, dpi);

        BBoxMapBounds linear = new BBoxMapBounds(SPHERICAL_MERCATOR, originalBBox.getMinX(), originalBBox.getMinY(),
                originalBBox.getMaxX(), originalBBox.getMaxY());

        final MapBounds newMapBounds = linear.adjustBoundsToNearestScale(zoomLevels, 0.05,
                ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE, true, screen, dpi);
        ReferencedEnvelope newBBox = newMapBounds.toReferencedEnvelope(screen, dpi);

        final double delta = 0.00001;
        assertEquals(originalBBox.getMedian(0), newBBox.getMedian(0), delta);
        assertEquals(originalBBox.getMedian(1), newBBox.getMedian(1), delta);

        assertEquals(399664, newBBox.getMinX(), 1);
        assertEquals(4999664, newBBox.getMinY(), 1);
        assertEquals(400335, newBBox.getMaxX(), 1);
        assertEquals(5000335, newBBox.getMaxY(), 1);
        assertEquals(26428, newMapBounds.getScaleDenominator(screen, dpi).getDenominator(), 1);
        assertEquals(20000, newMapBounds.getGeodeticScaleDenominator(screen, dpi).getDenominator(), delta);
    }
}
