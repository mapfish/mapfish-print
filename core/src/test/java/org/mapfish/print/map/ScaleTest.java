package org.mapfish.print.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mapfish.print.Constants.PDF_DPI;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class ScaleTest {
  public static final CoordinateReferenceSystem SPHERICAL_MERCATOR;
  public static final CoordinateReferenceSystem CH1903;
  public static final CoordinateReferenceSystem CH2056;
  private static final double DELTA = 0.00001;
  private static final double SCALE = 108335.72891406555;
  private static final double RESOLUTION = 38.21843770023979;

  static {
    try {
      SPHERICAL_MERCATOR = CRS.decode("EPSG:3857");
      CH1903 = CRS.decode("EPSG:21781");
      CH2056 = CRS.decode("EPSG:2056");
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

  /**
   * This test verifies that the geodetic scale calculation correctly uses position.x for the
   * horizontal coordinate. In projections like CH2056 (Swiss Oblique Mercator) or UTM, shifting a
   * point in X direction while keeping Y constant affects the geodetic distance, unlike EPSG:3857
   * where distortion is primarily latitude-dependent.
   *
   * <p>With some other projection like EPSG:5936 WGS 84 / EPSG Alaska Polar Stereographic The
   * defferance will consequant.
   */
  @Test
  public void geodeticCH2056XPositionDependence() {
    final Scale scale = new Scale(25000.0, CH2056, PDF_DPI);

    // Calculate geodetic scale at two positions with same Y but different X
    // Using coordinates in CH2056 CRS (EPSG:2056)
    final Coordinate positionRef = new Coordinate(2600000.0, 1200000.0);
    final Coordinate positionLeft = new Coordinate(2484274.0, 1200000.0);

    final double geodeticScaleRef = scale.getGeodeticDenominator(CH2056, PDF_DPI, positionRef);
    final double geodeticScaleLeft = scale.getGeodeticDenominator(CH2056, PDF_DPI, positionLeft);

    assertEquals(24996.77190700038, geodeticScaleRef, 0.0);
    assertEquals(24996.76743516703, geodeticScaleLeft, 0.0);
  }
}
