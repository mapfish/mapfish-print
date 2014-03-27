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
import org.geotools.renderer.lite.RendererUtilities;
import org.junit.Test;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;

/**
 * @author Jesse on 3/26/14.
 */
public class CenterScaleMapBoundsTest {
    private static final CoordinateReferenceSystem CH1903;
    private static final CoordinateReferenceSystem GOOGLE;
    static {
        try {
            CH1903 = CRS.decode("EPSG:2056");
            GOOGLE = CRS.decode("EPSG:3857");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testToReferencedEnvelope() throws Exception {
        final Scale startScale = new Scale(5000, DistanceUnit.IN);
        final CenterScaleMapBounds bounds = new CenterScaleMapBounds(CH1903, 10000, 10000, startScale);
        final int dpi = 90;
        final Rectangle paintArea = new Rectangle(500, 500);
        final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea, dpi);

        final double scale = RendererUtilities.calculateScale(envelope, paintArea.width, paintArea.height, dpi);

        assertEquals(startScale.getDenominator(), scale, 0.001);
    }
}
