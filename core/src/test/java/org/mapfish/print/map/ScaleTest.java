package org.mapfish.print.map;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.Constants.PDF_DPI;
import static org.mapfish.print.attribute.map.CenterScaleMapBoundsTest.CH1903;

public class ScaleTest{
    private static final double DELTA = 0.00001;
    private static final double SCALE = 108335.72891406555;
    private static final double RESOLUTION = 38.21843770023979;
    public static final CoordinateReferenceSystem SPHERICAL_MERCATOR;
    public static final CoordinateReferenceSystem CH1903;

    static {
        try {
            SPHERICAL_MERCATOR = CRS.decode("EPSG:3857");
            CH1903 = CRS.decode("EPSG:21781");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testToResolution() throws Exception {
        final double resolution = new Scale(SCALE, CH1903, PDF_DPI).getResolution();
        assertEquals(RESOLUTION, resolution, DELTA);
        assertEquals(SCALE, Scale.fromResolution(resolution).getDenominator(CH1903, PDF_DPI), DELTA);
    }

    @Test
    public void fromResolution() {
        assertEquals(SCALE, Scale.fromResolution(RESOLUTION).getDenominator(CH1903, PDF_DPI), DELTA);
    }

    @Test
    public void geodetic() throws Exception {
        final Scale scale = new Scale(15432.0, SPHERICAL_MERCATOR, 254);

        assertEquals(
                scale.getDenominator(SPHERICAL_MERCATOR, 254),
                15432.0, 1.0);
        assertEquals(
                scale.getGeodeticDenominator(SPHERICAL_MERCATOR, 254, new Coordinate(682433.0, 6379270.0)),
                10019.0, 1.0);
    }
}
