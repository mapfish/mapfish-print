package org.mapfish.print.map;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.Constants.PDF_DPI;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

public class ScaleTest {
  public static final CoordinateReferenceSystem SPHERICAL_MERCATOR;
  public static final CoordinateReferenceSystem CH1903;
  private static final double DELTA = 0.00001;
  private static final double SCALE = 108335.72891406555;
  private static final double RESOLUTION = 38.21843770023979;

  static {
    try {
      SPHERICAL_MERCATOR = CRS.decode("EPSG:3857");
      CH1903 = CRS.decode("EPSG:21781");
    } catch (FactoryException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testToResolution() {
    final double resolution = new Scale(SCALE, CH1903, PDF_DPI).getResolution();
    assertEquals(RESOLUTION, resolution, DELTA);
    assertEquals(SCALE, Scale.fromResolution(resolution, CH1903).getDenominator(PDF_DPI), DELTA);
  }

  @Test
  public void fromResolution() {
    assertEquals(SCALE, Scale.fromResolution(RESOLUTION, CH1903).getDenominator(PDF_DPI), DELTA);
  }

  @Test
  public void geodetic() {
    final Scale scale = new Scale(15432.0, SPHERICAL_MERCATOR, 254);

    assertEquals(scale.getDenominator(254), 15432.0, 1.0);
    assertEquals(
        scale.getGeodeticDenominator(SPHERICAL_MERCATOR, 254, new Coordinate(682433.0, 6379270.0)),
        10019.0,
        1.0);
  }

  @Test
  public void testStaticGetGeodeticDenominator() {
    final double scaleDenominator = 15432.0;
    final Coordinate position = new Coordinate(682433.0, 6379270.0);
    final double dpi = 254;

    // Test the static method
    final double result =
        Scale.getGeodeticDenominator(scaleDenominator, SPHERICAL_MERCATOR, dpi, position);

    // Verify the result is the same as calling the instance method
    final Scale scale = new Scale(scaleDenominator, SPHERICAL_MERCATOR, dpi);
    final double expected = scale.getGeodeticDenominator(SPHERICAL_MERCATOR, dpi, position);

    assertEquals(expected, result, 1.0);
    assertEquals(10019.0, result, 1.0);
  }
}
