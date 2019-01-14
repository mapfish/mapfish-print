package org.mapfish.print.map.geotools.grid;

import org.junit.Test;
import si.uom.NonSI;

import static org.junit.Assert.assertEquals;

public class GridUtilsTest {

    @Test
    public void testCreateLabel() {
        String degree = NonSI.DEGREE_ANGLE.toString();

        assertEquals(
                "100000 m", GridUtils.createLabel(100000, "m", null));
        assertEquals(
                "100000 m", GridUtils.createLabel(100000.12345, "m", null));
        assertEquals(
                "49.123457 " + degree, GridUtils.createLabel(49.12345678, degree, null));
        assertEquals(
                "100,000 m", GridUtils.createLabel(100000, "m", new GridLabelFormat.Simple("%,1.0f %s")));
        assertEquals(
                "100,000 m",
                GridUtils
                        .createLabel(100000, "m", new GridLabelFormat.Detailed("###,###", null, null, null)));
        assertEquals(
                "100'000 m",
                GridUtils.createLabel(100000, "m", new GridLabelFormat.Detailed("###,###", null, null, "'")));
        assertEquals(
                "100,000 m",
                GridUtils.createLabel(100000.123, "m",
                                      new GridLabelFormat.Detailed("###,###", null, null, null)));
        assertEquals(
                "100'000,12 m",
                GridUtils.createLabel(100000.123, "m",
                                      new GridLabelFormat.Detailed("###,###.##", null, ",", "'")));
    }

}
