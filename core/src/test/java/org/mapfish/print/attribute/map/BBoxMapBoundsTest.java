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
import org.junit.Test;
import org.mapfish.print.map.Scale;

import java.awt.Rectangle;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;
import static org.mapfish.print.attribute.map.CenterScaleMapBoundsTest.CH1903;

/**
 * @author Jesse on 3/27/14.
 */
public class BBoxMapBoundsTest {
    @Test(expected = IllegalArgumentException.class)
    public void testToReferencedEnvelopeMismatchAspectRatio() throws Exception {
        final BBoxMapBounds bboxMapBounds = new BBoxMapBounds(WGS84, 0, 0, 10, 10);

        bboxMapBounds.toReferencedEnvelope(new Rectangle(5, 10), 90);
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
                ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE, screen, dpi);
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
                ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE, screen, dpi);
        ReferencedEnvelope newBBox = newMapBounds.toReferencedEnvelope(screen, dpi);

        final double delta = 0.00001;
        assertEquals(originalBBox.getMedian(0), newBBox.getMedian(0), delta);
        assertEquals(originalBBox.getMedian(1), newBBox.getMedian(1), delta);

        double expectedScale = 25000;
        CenterScaleMapBounds expectedMapBounds = new CenterScaleMapBounds(WGS84, originalBBox.centre().x, originalBBox.centre().y,
                new Scale(expectedScale));
        assertEquals(expectedMapBounds.toReferencedEnvelope(screen, dpi), newBBox);
    }
}
