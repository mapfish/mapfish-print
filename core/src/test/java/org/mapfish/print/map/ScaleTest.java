package org.mapfish.print.map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.Constants.PDF_DPI;
import static org.mapfish.print.attribute.map.CenterScaleMapBoundsTest.CH1903;

/**
 * @author Jesse on 4/8/2014.
 */
public class ScaleTest{
    private static final double DELTA = 0.00001;
    private static final double SCALE = 108335.72891406555;
    private static final double RESOLUTION = 38.21843770023979;
    @Test
    public void testToResolution() throws Exception {
        final double resolution = new Scale(SCALE).toResolution(CH1903, PDF_DPI);
        assertEquals(RESOLUTION, resolution, DELTA);
        assertEquals(SCALE, Scale.fromResolution(resolution, CH1903, PDF_DPI).getDenominator(), DELTA);
    }

    @Test
    public void fromResolution() {
        assertEquals(SCALE, Scale.fromResolution(RESOLUTION, CH1903, PDF_DPI).getDenominator(), DELTA);
    }
}
