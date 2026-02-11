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
  public void geodeticCH1903XPositionDependence() {
    // This test verifies that the geodetic scale calculation correctly uses position.x
    // for the horizontal coordinate. In projections like CH1903 (Swiss Oblique Mercator)
    // or UTM, shifting a point in X direction while keeping Y constant affects the
    // geodetic distance, unlike EPSG:3857 where distortion is primarily latitude-dependent.
    final Scale scale = new Scale(25000.0, CH1903, PDF_DPI);

    // Calculate geodetic scale at two positions with same Y but different X
    // Using coordinates in CH1903 CRS (EPSG:21781) for Switzerland
    final Coordinate position1 = new Coordinate(600000.0, 200000.0);
    final Coordinate position2 = new Coordinate(700000.0, 200000.0);

    final double geodeticScale1 = scale.getGeodeticDenominator(CH1903, PDF_DPI, position1);
    final double geodeticScale2 = scale.getGeodeticDenominator(CH1903, PDF_DPI, position2);

    // The geodetic scales should be different when X position changes
    // If position.y was incorrectly used for both coordinates, they would be the same
    // Allow a tolerance, but the difference should be noticeable
    final double difference = Math.abs(geodeticScale1 - geodeticScale2);
    // Expecting at least 100 units difference in scale denominator for this shift
    assert difference > 100.0
        : String.format(
            "Expected significant difference in geodetic scale when X changes "
                + "(got %.2f vs %.2f, difference %.2f)",
            geodeticScale1, geodeticScale2, difference);
  }
}
