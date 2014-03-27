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
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;

/**
 * @author Jesse on 3/27/14.
 */
public class BBoxMapBoundsTest {
    @Test(expected = IllegalArgumentException.class)
    public void testToReferencedEnvelopeMismatchAspectRatio() throws Exception {
        final BBoxMapBounds bboxMapBounds = new BBoxMapBounds(DefaultGeographicCRS.WGS84, 0, 0, 10, 10);

        bboxMapBounds.toReferencedEnvelope(new Rectangle(5, 10), 90);
    }
    @Test
    public void testToReferencedEnvelope() throws Exception {
        final BBoxMapBounds bboxMapBounds = new BBoxMapBounds(DefaultGeographicCRS.WGS84, -180, -90, 180, 90);

        final ReferencedEnvelope envelope = bboxMapBounds.toReferencedEnvelope(new Rectangle(10, 5), 90);


        assertEquals(-180, envelope.getMinX(), 0.001);
        assertEquals(180, envelope.getMaxX(), 0.001);
        assertEquals(-90, envelope.getMinY(), 0.001);
        assertEquals(90, envelope.getMaxY(), 0.001);
        assertEquals(DefaultGeographicCRS.WGS84, envelope.getCoordinateReferenceSystem());
    }

}
