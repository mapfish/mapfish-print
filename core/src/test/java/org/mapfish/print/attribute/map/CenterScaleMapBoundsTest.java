package org.mapfish.print.attribute.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Rectangle;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Test;

/** Test bounds implementation. */
public class CenterScaleMapBoundsTest {
  public static final CoordinateReferenceSystem SPHERICAL_MERCATOR;
  public static final CoordinateReferenceSystem CH1903;
  public static final CoordinateReferenceSystem LAMBERT;
  static final double OPENLAYERS_2_DPI = 72;

  static {
    try {
      SPHERICAL_MERCATOR = CRS.decode("EPSG:3857");
      CH1903 = CRS.decode("EPSG:21781");
      LAMBERT = CRS.decode("EPSG:2154");
    } catch (FactoryException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testToReferencedEnvelopeCH1903Projection() {
    final double startScaleDenominator = 18984.396150703426;
    final CenterScaleMapBounds bounds =
        new CenterScaleMapBounds(CH1903, 659596.5, 185610.5, startScaleDenominator);
    final Rectangle paintArea = new Rectangle(521, 330);
    final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea);

    // It would be nice to nail this down in the future to the exact value but the method I used for
    // measurement was openlayers and
    // I don't know what the DPI it was using and I don't know how accurate its calculation is
    // either.
    assertEquals(657851d, envelope.getMinX(), 1);
    assertEquals(661341d, envelope.getMaxX(), 1);
    assertEquals(184505d, envelope.getMinY(), 1);
    assertEquals(186715d, envelope.getMaxY(), 1);
    assertEquals(CH1903, envelope.getCoordinateReferenceSystem());
  }

  @Test
  public void testToReferencedEnvelopeLambertProjection() {
    final double startScaleDenominator = 17983.582534790035;
    final CenterScaleMapBounds bounds =
        new CenterScaleMapBounds(LAMBERT, 445000, 6355000, startScaleDenominator);
    final Rectangle paintArea = new Rectangle(418, 512);
    final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea);

    // It would be nice to nail this down in the future to the exact value but the method I used for
    // measurement was openlayers and
    // I don't know what the DPI it was using and I don't know how accurate its calculation is
    // either.
    assertEquals(443674d, envelope.getMinX(), 1);
    assertEquals(446325d, envelope.getMaxX(), 1);
    assertEquals(6353375d, envelope.getMinY(), 1);
    assertEquals(6356624d, envelope.getMaxY(), 1);
    assertEquals(LAMBERT, envelope.getCoordinateReferenceSystem());
  }

  @Test
  public void testToReferencedEnvelopeLatLong() {
    final double startScaleDenominator = 56304.83087498591;
    final CenterScaleMapBounds bounds =
        new CenterScaleMapBounds(
            DefaultGeographicCRS.WGS84, 8.2335427805083, 46.801424340241, startScaleDenominator);
    final Rectangle paintArea = new Rectangle(521, 330);
    final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea);

    // It would be nice to nail this down in the future to the exact value but the method I used for
    // measurement was openlayers and
    // I don't know what the DPI it was using and I don't know how accurate its calculation is
    // either.
    final double delta = 0.000001;
    assertEquals(8.1657602, envelope.getMinX(), delta);
    assertEquals(8.3013252, envelope.getMaxX(), delta);
    assertEquals(46.771942, envelope.getMinY(), delta);
    assertEquals(46.830906, envelope.getMaxY(), delta);
    assertEquals(DefaultGeographicCRS.WGS84, envelope.getCoordinateReferenceSystem());
  }

  @Test
  public void testZoomOut() {
    final double Denominator = 2500.0;
    final CenterScaleMapBounds bounds =
        new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 0.0, 0.0, Denominator);
    final Rectangle paintArea = new Rectangle(400, 200);
    final ReferencedEnvelope envelope = bounds.toReferencedEnvelope(paintArea);

    CenterScaleMapBounds newBounds = bounds.zoomOut(1);
    ReferencedEnvelope newEnvelope = newBounds.toReferencedEnvelope(paintArea);

    final double delta = 0.000001;
    assertEquals(envelope.getMinX(), newEnvelope.getMinX(), delta);
    assertEquals(envelope.getMaxX(), newEnvelope.getMaxX(), delta);
    assertEquals(envelope.getMinY(), newEnvelope.getMinY(), delta);
    assertEquals(envelope.getMaxY(), newEnvelope.getMaxY(), delta);

    newBounds = bounds.zoomOut(2);
    newEnvelope = newBounds.toReferencedEnvelope(paintArea);

    assertEquals(envelope.getMinX() * 2, newEnvelope.getMinX(), delta);
    assertEquals(envelope.getMaxX() * 2, newEnvelope.getMaxX(), delta);
    assertEquals(envelope.getMinY() * 2, newEnvelope.getMinY(), delta);
    assertEquals(envelope.getMaxY() * 2, newEnvelope.getMaxY(), delta);
  }

  @Test
  public void testRollLongitude() {
    final double denominator = 10000.0;
    final CenterScaleMapBounds bounds =
        new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 0.0, 0.0, denominator);

    assertEquals(0.0, bounds.rollLongitude(0.0), 1e-6);
    assertEquals(180.0, bounds.rollLongitude(-180.0), 1e-6);
    assertEquals(180.0, bounds.rollLongitude(180.0), 1e-6);
    assertEquals(-179.0, bounds.rollLongitude(181.0), 1e-6);
    assertEquals(179.0, bounds.rollLongitude(-181.0), 1e-6);
    assertEquals(1.0, bounds.rollLongitude(361.0), 1e-6);
    assertEquals(-1.0, bounds.rollLongitude(-361.0), 1e-6);
  }

  @Test
  public void testRollLatitude() {
    final double denominator = 10000.0;
    final CenterScaleMapBounds bounds =
        new CenterScaleMapBounds(DefaultGeographicCRS.WGS84, 0.0, 0.0, denominator);

    assertEquals(0.0, bounds.rollLatitude(0.0), 1e-6);
    assertEquals(90.0, bounds.rollLatitude(90.0), 1e-6);
    assertEquals(-90.0, bounds.rollLatitude(-90.0), 1e-6);
    assertThrows(IllegalArgumentException.class, () -> bounds.rollLatitude(91.0));
    assertThrows(IllegalArgumentException.class, () -> bounds.rollLatitude(-91.0));
    assertThrows(IllegalArgumentException.class, () -> bounds.rollLatitude(180.0));
    assertThrows(IllegalArgumentException.class, () -> bounds.rollLatitude(-180.0));
    assertThrows(IllegalArgumentException.class, () -> bounds.rollLatitude(181.0));
    assertThrows(IllegalArgumentException.class, () -> bounds.rollLatitude(-181.0));
  }
}
