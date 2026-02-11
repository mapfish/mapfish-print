package org.mapfish.print.attribute.map;

import static org.mapfish.print.Constants.PDF_DPI;

import com.google.common.annotations.VisibleForTesting;
import java.awt.Rectangle;
import java.util.Objects;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.mapfish.print.FloatingPointUtil;
import org.mapfish.print.PrintException;
import org.mapfish.print.PseudoMercatorUtils;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;

/**
 * Represent Map Bounds with a center location and a scale of the map.
 *
 * <p>Created by Jesse on 3/26/14.
 */
public final class CenterScaleMapBounds extends MapBounds {
  private final Coordinate center;
  private final Scale scale;

  /**
   * Constructor.
   *
   * @param projection the projection these bounds are defined in.
   * @param centerX the x coordinate of the center point.
   * @param centerY the y coordinate of the center point.
   * @param scaleDenominator the scale denominator of the map.
   */
  public CenterScaleMapBounds(
      final CoordinateReferenceSystem projection,
      final double centerX,
      final double centerY,
      final double scaleDenominator) {
    this(projection, centerX, centerY, scaleDenominator, false);
  }

  /**
   * Constructor.
   *
   * @param projection the projection these bounds are defined in.
   * @param centerX the x coordinate of the center point.
   * @param centerY the y coordinate of the center point.
   * @param scaleDenominator the scale denominator of the map.
   * @param useGeodeticCalculations force to use geodetic calculations in PseudoMercator projection
   */
  public CenterScaleMapBounds(
      final CoordinateReferenceSystem projection,
      final double centerX,
      final double centerY,
      final double scaleDenominator,
      final boolean useGeodeticCalculations) {
    this(
        projection,
        centerX,
        centerY,
        new Scale(scaleDenominator, projection, PDF_DPI),
        useGeodeticCalculations);
  }

  /**
   * Constructor.
   *
   * @param projection the projection these bounds are defined in.
   * @param centerX the x coordinate of the center point.
   * @param centerY the y coordinate of the center point.
   * @param scale the scale of the map.
   */
  public CenterScaleMapBounds(
      final CoordinateReferenceSystem projection,
      final double centerX,
      final double centerY,
      final Scale scale) {
    this(projection, centerX, centerY, scale, false);
  }

  /**
   * Constructor.
   *
   * @param projection the projection these bounds are defined in.
   * @param centerX the x coordinate of the center point.
   * @param centerY the y coordinate of the center point.
   * @param scale the scale of the map.
   * @param useGeodeticCalculations force to use geodetic calculations in PseudoMercator projection
   */
  public CenterScaleMapBounds(
      final CoordinateReferenceSystem projection,
      final double centerX,
      final double centerY,
      final Scale scale,
      final boolean useGeodeticCalculations) {
    super(projection, useGeodeticCalculations);
    this.center = new Coordinate(centerX, centerY);
    this.scale = scale;
  }

  @Override
  public ReferencedEnvelope toReferencedEnvelope(final Rectangle paintArea) {
    ReferencedEnvelope bbox;
    CoordinateReferenceSystem crs = getProjection();
    final DistanceUnit projectionUnit = DistanceUnit.fromProjection(crs);
    if (projectionUnit == DistanceUnit.DEGREES) {
      double geoWidthInches = this.scale.getResolutionInInches() * paintArea.width;
      double geoHeightInches = this.scale.getResolutionInInches() * paintArea.height;
      bbox = computeGeodeticBBox(geoWidthInches, geoHeightInches);

    } else if (this.useGeodeticCalculations() && PseudoMercatorUtils.isPseudoMercator(crs)) {
      double geoWidthInM = scale.getResolution() * paintArea.width;
      double geoHeightInM = scale.getResolution() * paintArea.height;
      bbox = computeGeodeticBBoxInPseudoMercator(geoWidthInM, geoHeightInM);

    } else {
      final double centerX = this.center.getOrdinate(0);
      final double centerY = this.center.getOrdinate(1);

      double geoWidth = this.scale.getResolution() * paintArea.width;
      double geoHeight = this.scale.getResolution() * paintArea.height;

      double minGeoX = centerX - (geoWidth / 2.0);
      double minGeoY = centerY - (geoHeight / 2.0);
      double maxGeoX = minGeoX + geoWidth;
      double maxGeoY = minGeoY + geoHeight;
      bbox = new ReferencedEnvelope(minGeoX, maxGeoX, minGeoY, maxGeoY, crs);
    }

    return bbox;
  }

  @Override
  public MapBounds adjustedEnvelope(final Rectangle paintArea) {
    return this;
  }

  @Override
  public MapBounds adjustBoundsToNearestScale(
      final ZoomLevels zoomLevels,
      final double tolerance,
      final ZoomLevelSnapStrategy zoomLevelSnapStrategy,
      final boolean geodetic,
      final Rectangle paintArea,
      final double dpi) {

    final Scale newScale =
        getNearestScale(zoomLevels, tolerance, zoomLevelSnapStrategy, geodetic, paintArea, dpi);

    return new CenterScaleMapBounds(
        getProjection(), this.center.x, this.center.y, newScale, this.useGeodeticCalculations());
  }

  @Override
  public Scale getScale(final Rectangle paintArea, final double dpi) {
    return this.scale;
  }

  @Override
  public MapBounds adjustBoundsToRotation(final double rotation) {
    // nothing to change when rotating, because the center stays the same
    return this;
  }

  @Override
  public CenterScaleMapBounds zoomOut(final double factor) {
    if (FloatingPointUtil.equals(factor, 1.0)) {
      return this;
    }

    final double newResolution = this.scale.getResolution() * factor;
    return new CenterScaleMapBounds(
        getProjection(),
        this.center.x,
        this.center.y,
        this.scale.toResolution(newResolution),
        this.useGeodeticCalculations());
  }

  @Override
  public MapBounds zoomToScale(final Scale newScale) {
    return new CenterScaleMapBounds(
        getProjection(), this.center.x, this.center.y, newScale, this.useGeodeticCalculations());
  }

  @Override
  public Coordinate getCenter() {
    return this.center;
  }

  /**
   * Computes a {@link ReferencedEnvelope} representing the geographic extent for a map in
   * Pseudo-Mercator projection from given metric dimensions.
   *
   * <p>This method calculates a real-world geographic bounding box starting from a center
   * coordinate and using the specified width and height in meters. It accounts for Earth's
   * curvature and the properties of the Coordinate Reference System's ellipsoid through geodetic
   * calculations.
   *
   * <p>The geodetic computation involves:
   *
   * <ol>
   *   <li>Converting the metric dimensions to the units of the CRS's ellipsoid.
   *   <li>Using a {@link GeodeticCalculator} to determine the min/max geographic coordinates by
   *       calculating distances from the center point towards the west, east, north, and south.
   * </ol>
   *
   * <p>This method is particularly suited for Web Mercator (EPSG:3857) and similar projections
   * where geodetic calculations are more accurate than linear approximation.
   *
   * @param geoWidthInM The width of the bounding box in meters.
   * @param geoHeightInM The height of the bounding box in meters.
   * @return The calculated {@link ReferencedEnvelope} representing the map's geographic extent.
   * @throws PrintException if the geodetic calculation fails or the coordinate transformation is
   *     invalid.
   */
  private ReferencedEnvelope computeGeodeticBBoxInPseudoMercator(
      final double geoWidthInM, final double geoHeightInM) {
    try {
      CoordinateReferenceSystem crs = this.getProjection();

      GeodeticCalculator calc = new GeodeticCalculator(crs);

      DistanceUnit ellipsoidUnit =
          DistanceUnit.fromString(calc.getEllipsoid().getAxisUnit().toString());
      double geoWidth = DistanceUnit.M.convertTo(geoWidthInM, ellipsoidUnit);
      double geoHeight = DistanceUnit.M.convertTo(geoHeightInM, ellipsoidUnit);

      Position2D directPosition2D = new Position2D(this.center.x, this.center.y);
      directPosition2D.setCoordinateReferenceSystem(crs);
      calc.setStartingPosition(directPosition2D);

      final int west = -90;
      calc.setDirection(west, geoWidth / 2.0);
      double minGeoX = calc.getDestinationPosition().getOrdinate(0);

      final int east = 90;
      calc.setDirection(east, geoWidth / 2.0);
      double maxGeoX = calc.getDestinationPosition().getOrdinate(0);

      final int south = 180;
      calc.setDirection(south, geoHeight / 2.0);
      double southHeight = calc.getOrthodromicDistance();

      final int north = 0;
      calc.setDirection(north, geoHeight / 2.0);
      double northHeight = calc.getOrthodromicDistance();

      double halfHeight = (southHeight + northHeight) / 2;
      double minGeoY = calc.getStartingPosition().getOrdinate(1) - halfHeight;
      double maxGeoY = calc.getStartingPosition().getOrdinate(1) + halfHeight;

      return new ReferencedEnvelope(minGeoX, maxGeoX, minGeoY, maxGeoY, crs);
    } catch (TransformException e) {
      throw new PrintException("Failed to compute geodetic bbox for pseudo-mercator projection", e);
    }
  }

  private ReferencedEnvelope computeGeodeticBBox(
      final double geoWidthInInches, final double geoHeightInInches) {
    try {
      CoordinateReferenceSystem crs = getProjection();

      GeodeticCalculator calc = new GeodeticCalculator(crs);

      DistanceUnit ellipsoidUnit =
          DistanceUnit.fromString(calc.getEllipsoid().getAxisUnit().toString());
      double geoWidth = DistanceUnit.IN.convertTo(geoWidthInInches, ellipsoidUnit);
      double geoHeight = DistanceUnit.IN.convertTo(geoHeightInInches, ellipsoidUnit);

      Position2D directPosition2D = new Position2D(this.center.x, this.center.y);
      directPosition2D.setCoordinateReferenceSystem(crs);
      calc.setStartingPosition(directPosition2D);

      final int west = -90;
      calc.setDirection(west, geoWidth / 2.0);
      double minGeoX = calc.getDestinationPosition().getOrdinate(0);

      final int east = 90;
      calc.setDirection(east, geoWidth / 2.0);
      double maxGeoX = calc.getDestinationPosition().getOrdinate(0);

      final int south = 180;
      calc.setDirection(south, geoHeight / 2.0);
      double minGeoY = calc.getDestinationPosition().getOrdinate(1);

      final int north = 0;
      calc.setDirection(north, geoHeight / 2.0);
      double maxGeoY = calc.getDestinationPosition().getOrdinate(1);

      return new ReferencedEnvelope(
          rollLongitude(minGeoX),
          rollLongitude(maxGeoX),
          rollLatitude(minGeoY),
          rollLatitude(maxGeoY),
          crs);
    } catch (TransformException e) {
      throw new PrintException("Failed to compute geodetic bbox", e);
    }
  }

  @VisibleForTesting
  double rollLongitude(final double x) {
    return modulo(x + 180, 360.0) - 180;
  }

  private static double modulo(final double a, final double b) {
    return b - ((-a % b) + b) % b;
  }

  @VisibleForTesting
  double rollLatitude(final double y) {
    if (y > 90 || y < -90) {
      throw new IllegalArgumentException("Latitude must be between -90 and 90");
    }
    return y;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    final CenterScaleMapBounds that = (CenterScaleMapBounds) o;

    if (!Objects.equals(this.center, that.center)) {
      return false;
    }
    return Objects.equals(this.scale, that.scale);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (this.center != null ? this.center.hashCode() : 0);
    result = 31 * result + (this.scale != null ? this.scale.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format("CenterScaleMapBounds{center=%s, scale=%s}", this.center, this.scale);
  }
}
