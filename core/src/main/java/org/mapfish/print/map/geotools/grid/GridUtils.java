package org.mapfish.print.map.geotools.grid;

import static org.mapfish.print.map.geotools.grid.GridLabel.Side.BOTTOM;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.LEFT;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.RIGHT;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.TOP;

import jakarta.annotation.Nonnull;
import java.awt.geom.AffineTransform;
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

  /**
   * Calculate the rotation of the text for the given side and map rotation.
   *
   * @param mapRotation The rotation of the map (radians).
   * @param side The side of the grid/map where the label is placed.
   * @param rotateLabels Whether to rotate labels with the map lines.
   * @return The text rotation in radians (relative to screen X axis).
   */
  public static double calculateTextRotation(
      final double mapRotation, final GridLabel.Side side, final boolean rotateLabels) {
    if (!rotateLabels) {
      return 0.0;
    }

    double baseAngle;
    switch (side) {
      case BOTTOM:
      case TOP:
        // Vertical lines (North-South). Base vector (0, 1) or (0, -1).
        // Angle PI/2 or -PI/2.
        baseAngle = -Math.PI / 2; // Up/Down. Let's start with -90 (Up).
        break;
      default:
        // Horizontal lines (East-West). Base vector (1, 0) or (-1, 0).
        // Angle 0 or PI.
        baseAngle = 0.0;
    }

    // Rotate the base vector by mapRotation
    double angle = baseAngle + mapRotation;

    // Normalize angle to [-PI, PI]
    while (angle <= -Math.PI) {
      angle += 2 * Math.PI;
    }
    while (angle > Math.PI) {
      angle -= 2 * Math.PI;
    }

    // Adjust for readability: keep angle in (-PI/2, PI/2]
    // i.e. text reads Left-to-Right or Bottom-to-Top
    if (angle > Math.PI / 2) {
      angle -= Math.PI;
    } else if (angle <= -Math.PI / 2) {
      angle += Math.PI;
    }

    return angle;
  }

  /**
   * Apply indentation translation to the transform. The indentation is applied perpendicular to the
   * map border corresponding to the label side.
   *
   * @param transform The transform to update.
   * @param side The side of the map.
   * @param indent The indentation in pixels.
   * @param mapRotation The rotation of the map.
   */
  public static void applyIndent(
      final AffineTransform transform,
      final GridLabel.Side side,
      final int indent,
      final double mapRotation) {
    // Determine the "Outward" normal vector for the border in Screen Coordinates.
    // Base normals (for unrotated map):
    // BOTTOM (South): Down (Screen +Y)? No.
    // Map Coords: MinY is South. Screen: MaxY is Bottom.
    // Screen Base Normal: (0, 1) (Down).
    // WAIT. Previous analysis said (0, -1) Up was Inside?
    // If I want to indent OUTSIDE.
    // From Bottom Border (Screen MaxY), go Down (+Y).
    // From Top Border (Screen MinY), go Up (-Y).
    // From Left Border (Screen MinX), go Left (-X).
    // From Right Border (Screen MaxX), go Right (+X).

    double nx = 0;
    double ny = 0;

    switch (side) {
      case BOTTOM:
        nx = 0;
        ny = 1; // Down
        break;
      case TOP:
        nx = 0;
        ny = -1; // Up
        break;
      case LEFT:
        nx = -1; // Left
        ny = 0;
        break;
      case RIGHT:
        nx = 1; // Right
        ny = 0;
        break;
      default:
        break;
    }

    // Rotate the normal vector by mapRotation
    // transform.rotate() rotates axes. Vector coordinates rotate inversely?
    // No, we want the vector in Screen Coordinates.
    // The "Bottom" side of the map rotates with the map.
    // So the Normal rotates with the map.
    // Rotation is +angle (CW in Java2D).
    double cos = Math.cos(mapRotation);
    double sin = Math.sin(mapRotation);

    double rnx = nx * cos - ny * sin;
    double rny = nx * sin + ny * cos;

    // Apply translation
    transform.translate(indent * rnx, indent * rny);
  }

  /**
   * Align the text (Start/End/Center) based on the relationship between the text orientation and
   * the border orientation.
   */
  public static void alignText(
      final AffineTransform transform,
      final GridLabel.Side side,
      final double mapRotation,
      final double textRotation,
      final java.awt.geom.Rectangle2D textBounds,
      final int halfCharHeight) {

    // Calculate Indent Direction (Outward Normal)
    // Same logic as applyIndent, re-calculated or could be passed.
    double nx = 0;
    double ny = 0;
    switch (side) {
      case BOTTOM:
        nx = 0;
        ny = 1;
        break;
      case TOP:
        nx = 0;
        ny = -1;
        break;
      case LEFT:
        nx = -1;
        ny = 0;
        break;
      case RIGHT:
        nx = 1;
        ny = 0;
        break;
      default:
        break;
    }
    double cosMap = Math.cos(mapRotation);
    double sinMap = Math.sin(mapRotation);
    double rnx = nx * cosMap - ny * sinMap;
    double rny = nx * sinMap + ny * cosMap;

    // Calculate Text Direction vector
    double cosText = Math.cos(textRotation);
    double sinText = Math.sin(textRotation);

    // Dot product
    double dot = rnx * cosText + rny * sinText;

    // Align based on dot product
    // If dot > 0 (Text runs Outward): Align Start (Shift 0).
    // If dot < 0 (Text runs Inward): Align End (Shift -Width).
    // If dot ~ 0 (Text runs Parallel to Border): Center (Shift -Width/2).

    double width = textBounds.getWidth();
    double xShift = 0;

    final double threshold = 0.1; // Tolerance for perpendicular
    if (Math.abs(dot) < threshold) {
      xShift = -width / 2.0;
    } else if (dot < 0) {
      xShift = -width;
    }

    // Vertical Centering (Perpendicular to Text)
    // Usually we want to center the text vertically on the grid line/anchor.
    // halfCharHeight shifts baseline down to center.
    // But RotationQuadrant used complicated logic.
    // Let's assume standard centering.
    double yShift = halfCharHeight;

    // Apply translation (in Text Coordinates)
    transform.translate(xShift, yShift);
  }
}
