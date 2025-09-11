package org.mapfish.print.map.geotools.grid;

import static org.mapfish.print.map.geotools.grid.GridLabel.Side.BOTTOM;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.LEFT;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.RIGHT;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.TOP;

import java.awt.geom.AffineTransform;
import jakarta.annotation.Nonnull;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import si.uom.NonSI;

/** A set of methods shared between the different Grid strategies. */
final class GridUtils {
  private GridUtils() {
    // do nothing
  }

  /**
   * Create a polygon that represents in world space the exact area that will be visible on the
   * printed map.
   *
   * @param context map context
   */
  public static Polygon calculateBounds(final MapfishMapContext context) {
    double rotation = context.getRootContext().getRotation();
    ReferencedEnvelope env = context.getRootContext().toReferencedEnvelope();

    Coordinate centre = env.centre();
    AffineTransform rotateInstance =
        AffineTransform.getRotateInstance(rotation, centre.x, centre.y);

    double[] dstPts = new double[8];
    double[] srcPts = {
      env.getMinX(), env.getMinY(), env.getMinX(), env.getMaxY(),
      env.getMaxX(), env.getMaxY(), env.getMaxX(), env.getMinY()
    };

    rotateInstance.transform(srcPts, 0, dstPts, 0, 4);

    return new GeometryFactory()
        .createPolygon(
            new Coordinate[] {
              new Coordinate(dstPts[0], dstPts[1]), new Coordinate(dstPts[2], dstPts[3]),
              new Coordinate(dstPts[4], dstPts[5]), new Coordinate(dstPts[6], dstPts[7]),
              new Coordinate(dstPts[0], dstPts[1])
            });
  }

  /**
   * Calculate the where the grid first line should be drawn when spacing and origin are defined in
   * {@link GridParam}.
   *
   * @param bounds the map bounds
   * @param layerData the parameter information
   * @param ordinal the direction (x,y) the grid line will be drawn.
   */
  public static double calculateFirstLine(
      final ReferencedEnvelope bounds, final GridParam layerData, final int ordinal) {
    double spaceFromOrigin = bounds.getMinimum(ordinal) - layerData.origin[ordinal];
    double linesBetweenOriginAndMap = Math.ceil(spaceFromOrigin / layerData.spacing[ordinal]);

    return linesBetweenOriginAndMap * layerData.spacing[ordinal] + layerData.origin[ordinal];
  }

  /**
   * Create the grid feature type.
   *
   * @param mapContext the map context containing the information about the map the grid will be
   *     added to.
   * @param geomClass the geometry type
   */
  public static SimpleFeatureType createGridFeatureType(
      @Nonnull final MapfishMapContext mapContext,
      @Nonnull final Class<? extends Geometry> geomClass) {
    final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    CoordinateReferenceSystem projection = mapContext.getBounds().getProjection();
    typeBuilder.add(Constants.Style.Grid.ATT_GEOM, geomClass, projection);
    typeBuilder.setName(Constants.Style.Grid.NAME_LINES);

    return typeBuilder.buildFeatureType();
  }

  /**
   * Create the label for a grid line.
   *
   * @param value the value of the line
   * @param unit the unit that the value is in
   */
  public static String createLabel(
      final double value, final String unit, final GridLabelFormat format) {
    final double zero = 0.000000001;
    if (format != null) {
      return format.format(value, unit);
    } else {
      if (Math.abs(value - Math.round(value)) < zero) {
        return String.format("%d %s", Math.round(value), unit);
      } else if ("m".equals(unit)) {
        // meter: no decimals
        return String.format("%1.0f %s", value, unit);
      } else if (NonSI.DEGREE_ANGLE.toString().equals(unit)) {
        // degree: by default 6 decimals
        return String.format("%1.6f %s", value, unit);
      } else {
        return String.format("%f %s", value, unit);
      }
    }
  }

  /**
   * Create the affine transform that maps from the world (map) projection to the pixel on the
   * printed map.
   *
   * @param mapContext the map context
   */
  public static AffineTransform getWorldToScreenTransform(final MapfishMapContext mapContext) {
    return RendererUtilities.worldToScreenTransform(
        mapContext.toReferencedEnvelope(), mapContext.getPaintArea());
  }

  /**
   * Calculate the position and label of the top grid label and add it to the label collector.
   *
   * @param labels the label collector
   * @param unit the unit of the project, used to create label.
   * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
   * @param intersections
   */
  public static void topBorderLabel(
      final LabelPositionCollector labels,
      final String unit,
      final AffineTransform worldToScreenTransform,
      final MathTransform toLabelProjection,
      final GridLabelFormat labelFormat,
      final Geometry intersections) {

    if (intersections.getNumPoints() > 0) {
      Coordinate borderIntersection = intersections.getGeometryN(0).getCoordinates()[0];
      double[] screenPoints = new double[2];
      worldToScreenTransform.transform(
          new double[] {borderIntersection.x, borderIntersection.y}, 0, screenPoints, 0, 1);

      double[] labelProj = transformToLabelProjection(toLabelProjection, borderIntersection);
      labels.add(
          new GridLabel(
              createLabel(labelProj[0], unit, labelFormat),
              (int) screenPoints[0],
              (int) screenPoints[1],
              TOP));
    }
  }

  /**
   * Calculate the position and label of the bottom grid label and add it to the label collector.
   *
   * @param labels the label collector
   * @param unit the unit of the project, used to create label.
   * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
   * @param intersections
   */
  public static void bottomBorderLabel(
      final LabelPositionCollector labels,
      final String unit,
      final AffineTransform worldToScreenTransform,
      final MathTransform toLabelProjection,
      final GridLabelFormat labelFormat,
      final Geometry intersections) {

    if (intersections.getNumPoints() > 0) {
      double[] screenPoints = new double[2];
      double[] labelProj =
          getLabelProj(worldToScreenTransform, toLabelProjection, intersections, screenPoints);
      labels.add(
          new GridLabel(
              createLabel(labelProj[0], unit, labelFormat),
              (int) screenPoints[0],
              (int) screenPoints[1],
              BOTTOM));
    }
  }

  /**
   * Calculate the position and label of the right side grid label and add it to the label
   * collector.
   *
   * @param labels the label collector
   * @param unit the unit of the project, used to create label.
   * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
   * @param intersections
   */
  public static void rightBorderLabel(
      final LabelPositionCollector labels,
      final String unit,
      final AffineTransform worldToScreenTransform,
      final MathTransform toLabelProjection,
      final GridLabelFormat labelFormat,
      final Geometry intersections) {

    if (intersections.getNumPoints() > 0) {
      double[] screenPoints = new double[2];
      double[] labelProj =
          getLabelProj(worldToScreenTransform, toLabelProjection, intersections, screenPoints);
      labels.add(
          new GridLabel(
              createLabel(labelProj[1], unit, labelFormat),
              (int) screenPoints[0],
              (int) screenPoints[1],
              RIGHT));
    }
  }

  private static double[] getLabelProj(
      final AffineTransform worldToScreenTransform,
      final MathTransform toLabelProjection,
      final Geometry intersections,
      final double[] screenPoints) {
    int idx = intersections instanceof LineString ? 1 : 0;
    Coordinate borderIntersection = intersections.getGeometryN(0).getCoordinates()[idx];
    worldToScreenTransform.transform(
        new double[] {borderIntersection.x, borderIntersection.y}, 0, screenPoints, 0, 1);

    return transformToLabelProjection(toLabelProjection, borderIntersection);
  }

  /**
   * Calculate the position and label of the left side grid label and add it to the label collector.
   *
   * @param labels the label collector
   * @param unit the unit of the project, used to create label.
   * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
   * @param intersections
   */
  static void leftBorderLabel(
      final LabelPositionCollector labels,
      final String unit,
      final AffineTransform worldToScreenTransform,
      final MathTransform toLabelProjection,
      final GridLabelFormat labelFormat,
      final Geometry intersections) {

    if (intersections.getNumPoints() > 0) {
      double[] screenPoints = new double[2];
      Coordinate borderIntersection = intersections.getGeometryN(0).getCoordinates()[0];
      worldToScreenTransform.transform(
          new double[] {borderIntersection.x, borderIntersection.y}, 0, screenPoints, 0, 1);

      double[] labelProj = transformToLabelProjection(toLabelProjection, borderIntersection);

      labels.add(
          new GridLabel(
              createLabel(labelProj[1], unit, labelFormat),
              (int) screenPoints[0],
              (int) screenPoints[1],
              LEFT));
    }
  }

  private static double[] transformToLabelProjection(
      final MathTransform toLabelProjection, final Coordinate borderIntersection) {
    try {
      double[] labelProj = new double[2];
      toLabelProjection.transform(
          new double[] {borderIntersection.x, borderIntersection.y}, 0, labelProj, 0, 1);
      return labelProj;
    } catch (TransformException e) {
      throw new RuntimeException(e);
    }
  }

  static Geometry computeLeftBorderIntersections(
      final Polygon rotatedBounds, final GeometryFactory geometryFactory, final double y) {
    Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
    LineString lineString =
        geometryFactory.createLineString(
            new Coordinate[] {
              new Coordinate(envelopeInternal.getMinX(), y),
              new Coordinate(envelopeInternal.centre().x, y)
            });
    return lineString.intersection(rotatedBounds.getExteriorRing());
  }

  static Geometry computeTopBorderIntersections(
      final Polygon rotatedBounds, final GeometryFactory geometryFactory, final double x) {
    Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
    LineString lineString =
        geometryFactory.createLineString(
            new Coordinate[] {
              new Coordinate(x, envelopeInternal.centre().y),
              new Coordinate(x, envelopeInternal.getMaxY())
            });
    return lineString.intersection(rotatedBounds.getExteriorRing());
  }

  static Geometry computeBottomBorderIntersections(
      final Polygon rotatedBounds, final GeometryFactory geometryFactory, final double x) {
    Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
    LineString lineString =
        geometryFactory.createLineString(
            new Coordinate[] {
              new Coordinate(x, envelopeInternal.getMinY()),
              new Coordinate(x, envelopeInternal.centre().y)
            });
    return lineString.intersection(rotatedBounds.getExteriorRing());
  }

  static Geometry computeRightBorderIntersections(
      final Polygon rotatedBounds, final GeometryFactory geometryFactory, final double y) {
    Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
    final LineString lineString =
        geometryFactory.createLineString(
            new Coordinate[] {
              new Coordinate(envelopeInternal.centre().x, y),
              new Coordinate(envelopeInternal.getMaxX(), y)
            });
    return lineString.intersection(rotatedBounds.getExteriorRing());
  }
}
