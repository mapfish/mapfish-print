package org.mapfish.print.attribute.map;

import org.junit.Test;
import org.mapfish.print.map.DistanceUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mapfish.print.Constants.PDF_DPI;

public class ZoomLevelsTest {

    @Test
    public void testRemoveDuplicates() {
        final ZoomLevels zoomLevels = new ZoomLevels(4, 2, 3, 3, 2, 1, 3, 2, 1);

        assertEquals(4, zoomLevels.size());
        assertEquals(4, (int) zoomLevels.get(0, DistanceUnit.M).getDenominator(PDF_DPI));
        assertEquals(3, (int) zoomLevels.get(1, DistanceUnit.M).getDenominator(PDF_DPI));
        assertEquals(2, (int) zoomLevels.get(2, DistanceUnit.M).getDenominator(PDF_DPI));
        assertEquals(1, (int) zoomLevels.get(3, DistanceUnit.M).getDenominator(PDF_DPI));

    }

    @Test
    public void testSort() {

        final ZoomLevels zoomLevels = new ZoomLevels(4, 2, 3, 1);

        assertEquals(4, zoomLevels.size());
        assertEquals(4, (int) zoomLevels.get(0, DistanceUnit.M).getDenominator(PDF_DPI));
        assertEquals(3, (int) zoomLevels.get(1, DistanceUnit.M).getDenominator(PDF_DPI));
        assertEquals(2, (int) zoomLevels.get(2, DistanceUnit.M).getDenominator(PDF_DPI));
        assertEquals(1, (int) zoomLevels.get(3, DistanceUnit.M).getDenominator(PDF_DPI));

    }

    @Test
    public void testEquals() {

        final ZoomLevels zoomLevels1 = new ZoomLevels(4, 2, 3, 0);
        final ZoomLevels zoomLevels2 = new ZoomLevels(4, 2, 3, 1);
        final ZoomLevels zoomLevels3 = new ZoomLevels(1, 2, 3, 3, 3, 4);

        assertEquals(zoomLevels2, zoomLevels3);
        assertEquals(zoomLevels3, zoomLevels2);
        assertEquals(zoomLevels2, zoomLevels2);
        assertEquals(zoomLevels3, zoomLevels3);
        assertEquals(zoomLevels1, zoomLevels1);
        assertNotEquals(zoomLevels1, zoomLevels2);
        assertNotEquals(zoomLevels1, zoomLevels3);
        assertNotEquals(zoomLevels2, zoomLevels1);

    }
}
