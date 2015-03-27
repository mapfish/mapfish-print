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
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;

/**
 * Test bounds implementation.
 * @author Jesse on 3/26/14.
 */
public class CenterScaleMapBoundsTest {
    static final double OPENLAYERS_2_DPI = 72;
    public static final CoordinateReferenceSystem CH1903;
    public static final CoordinateReferenceSystem LAMBERT;

    static {
        try {
            CH1903 = CRS.decode("EPSG:21781");
            LAMBERT = CRS.decode("EPSG:2154");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testToReferencedEnvelopeCH1903Projection() throws Exception {
        final Scale startScale = new Scale(18984.396150703426);
        final CenterScaleMapBounds bounds = new CenterScaleMapBounds(CH1903, 659596.5, 185610.5, startScale);
        final Rectangle paintArea = new Rectangle(521, 330);
        final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea, OPENLAYERS_2_DPI);

        // It would be nice to nail this down in the future to the exact value but the method I used for measurement was openlayers and
        // I don't know what the DPI it was using and I don't know how accurate its calculation is either.
        assertEquals(657851, envelope.getMinX(), 1);
        assertEquals(661341, envelope.getMaxX(), 1);
        assertEquals(184505, envelope.getMinY(), 1);
        assertEquals(186715, envelope.getMaxY(), 1);
        assertEquals(CH1903, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testToReferencedEnvelopeLambertProjection() throws Exception {
        final Scale startScale = new Scale(17983.582534790035);
        final CenterScaleMapBounds bounds = new CenterScaleMapBounds(LAMBERT, 445000, 6355000, startScale);
        final Rectangle paintArea = new Rectangle(418, 512);
        final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea, OPENLAYERS_2_DPI);

        // It would be nice to nail this down in the future to the exact value but the method I used for measurement was openlayers and
        // I don't know what the DPI it was using and I don't know how accurate its calculation is either.
        assertEquals(443674, envelope.getMinX(), 1);
        assertEquals(446325, envelope.getMaxX(), 1);
        assertEquals(6353375, envelope.getMinY(), 1);
        assertEquals(6356624, envelope.getMaxY(), 1);
        assertEquals(LAMBERT, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testToReferencedEnvelopeLatLong() throws Exception {
        final Scale startScale = new Scale(56304.83087498591);
        final CenterScaleMapBounds bounds = new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 8.2335427805083, 46.801424340241,
                startScale);
        final Rectangle paintArea = new Rectangle(521, 330);
        final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea, OPENLAYERS_2_DPI);

        // It would be nice to nail this down in the future to the exact value but the method I used for measurement was openlayers and
        // I don't know what the DPI it was using and I don't know how accurate its calculation is either.
        final double delta = 0.000001;
        assertEquals(8.1657602, envelope.getMinX(), delta);
        assertEquals(8.3013252, envelope.getMaxX(), delta);
        assertEquals(46.771942, envelope.getMinY(), delta);
        assertEquals(46.830906, envelope.getMaxY(), delta);
        assertEquals(DefaultGeographicCRS.WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testZoomOut() throws Exception {
        final Scale scale = new Scale(2500.0);
        final CenterScaleMapBounds bounds = new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 0.0, 0.0, scale);
        final Rectangle paintArea = new Rectangle(400, 200);
        final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea, OPENLAYERS_2_DPI);
        
        CenterScaleMapBounds newBounds = bounds.zoomOut(1);
        ReferencedEnvelope newEnvelope = newBounds.toReferencedEnvelope(paintArea, OPENLAYERS_2_DPI);

        final double delta = 0.000001;
        assertEquals(envelope.getMinX(), newEnvelope.getMinX(), delta);
        assertEquals(envelope.getMaxX(), newEnvelope.getMaxX(), delta);
        assertEquals(envelope.getMinY(), newEnvelope.getMinY(), delta);
        assertEquals(envelope.getMaxY(), newEnvelope.getMaxY(), delta);
        
        newBounds = bounds.zoomOut(2);
        newEnvelope = newBounds.toReferencedEnvelope(paintArea, OPENLAYERS_2_DPI);

        assertEquals(envelope.getMinX() * 2, newEnvelope.getMinX(), delta);
        assertEquals(envelope.getMaxX() * 2, newEnvelope.getMaxX(), delta);
        assertEquals(envelope.getMinY() * 2, newEnvelope.getMinY(), delta);
        assertEquals(envelope.getMaxY() * 2, newEnvelope.getMaxY(), delta);
    }

    @Test
    public void reproject() throws Exception {
        final Scale scale = new Scale(2500.0);
        final CenterScaleMapBounds centerBounds = new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 0.0, 0.0, scale);
        CenterScaleMapBounds bounds = (CenterScaleMapBounds) centerBounds.reproject(CH1903);
        assertEquals(CH1903, bounds.getProjection());
    }

}
