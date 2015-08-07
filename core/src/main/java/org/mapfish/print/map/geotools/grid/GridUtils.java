package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.RendererUtilities;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.AffineTransform;
import javax.annotation.Nonnull;

import static org.mapfish.print.map.geotools.grid.GridLabel.Side.BOTTOM;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.LEFT;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.RIGHT;
import static org.mapfish.print.map.geotools.grid.GridLabel.Side.TOP;

/**
 * A set of methods shared between the different Grid strategies.
 *
 * @author Jesse on 7/10/2015.
 */
final class GridUtils {
    private GridUtils() {
        // do nothing
    }

    /**
     * Create a polygon that represents in world space the exact area that will be visible on
     * the printed map.
     *
     * @param context map context
     */
    public static Polygon calculateBounds(final MapfishMapContext context) {
        double rotation = context.getRootContext().getRotation();
        ReferencedEnvelope env = context.getRootContext().toReferencedEnvelope();

        Coordinate centre = env.centre();
        AffineTransform rotateInstance = AffineTransform.getRotateInstance(rotation, centre.x, centre.y);

        // CSOFF: MagicNumber
        double[] dstPts = new double[8];
        double[] srcPts = {
                env.getMinX(), env.getMinY(), env.getMinX(), env.getMaxY(),
                env.getMaxX(), env.getMaxY(), env.getMaxX(), env.getMinY()};

        rotateInstance.transform(srcPts, 0, dstPts, 0, 4);

        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(dstPts[0], dstPts[1]), new Coordinate(dstPts[2], dstPts[3]),
                new Coordinate(dstPts[4], dstPts[5]), new Coordinate(dstPts[6], dstPts[7]),
                new Coordinate(dstPts[0], dstPts[1])
        });
        // CSON: MagicNumber
    }

    /**
     * Calculate the where the grid first line should be drawn when spacing and origin are defined in {@link GridParam}.
     *
     * @param bounds the map bounds
     * @param layerData the parameter information
     * @param ordinal the direction (x,y) the grid line will be drawn.
     */
    public static double calculateFirstLine(final ReferencedEnvelope bounds,
                                     final GridParam layerData,
                                     final int ordinal) {
        double spaceFromOrigin = bounds.getMinimum(ordinal) - layerData.origin[ordinal];
        double linesBetweenOriginAndMap = Math.ceil(spaceFromOrigin / layerData.spacing[ordinal]);

        return linesBetweenOriginAndMap * layerData.spacing[ordinal] + layerData.origin[ordinal];
    }

    /**
     * Create the grid feature type.
     *
     * @param mapContext the map context containing the information about the map the grid will be added to.
     * @param geomClass  the geometry type
     */
    public static SimpleFeatureType createGridFeatureType(@Nonnull final MapfishMapContext mapContext,
                                                   @Nonnull final Class<? extends Geometry> geomClass) {
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        CoordinateReferenceSystem projection = mapContext.getBounds().getProjection();
        typeBuilder.add(Constants.Style.Grid.ATT_GEOM, geomClass, projection);
        typeBuilder.setName(Constants.Style.Grid.NAME_LINES);

        return typeBuilder.buildFeatureType();
    }

    /**
     * Create the label for the a grid line.
     *
     * @param value the value of the line
     * @param unit  the unit that the value is in
     */
    public static String createLabel(final double value, final String unit) {
        final double zero = 0.000000001;
        final int maxBeforeNoDecimals = 1000000;
        final double minBeforeScientific = 0.0001;
        final int maxWithDecimals = 1000;

        if (Math.abs(value - Math.round(value)) < zero) {
            return String.format("%d %s", Math.round(value), unit);
        } else {
            if (value > maxBeforeNoDecimals || value < minBeforeScientific) {
                return String.format("%1.0f %s", value, unit);
            } else if (value < maxWithDecimals) {
                return String.format("%1.2f %s", value, unit);
            } else if (value > minBeforeScientific) {
                return String.format("%1.4f %s", value, unit);
            } else {
                return String.format("%e %s", value, unit);
            }
        }
    }

    /**
     * Create the affine transform that maps from the world (map) projection to the pixel on the printed map.
     * @param mapContext the map context
     */
    public static AffineTransform getWorldToScreenTransform(final MapfishMapContext mapContext) {
        return RendererUtilities.worldToScreenTransform(mapContext.toReferencedEnvelope(),
                mapContext.getPaintArea());
    }

    /**
     * Calculate the position and label of the top grid label and add it to the label collector.
     *
     * @param labels the label collector
     * @param geometryFactory the geometry factory for creating JTS geometries
     * @param rotatedBounds the full bounds of the map taking rotation into account.
     * @param unit the unit of the project, used to create label.
     * @param x the x coordinate where the grid line is.
     * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
     */
    public static void topBorderLabel(final LabelPositionCollector labels, final GeometryFactory geometryFactory,
                               final Polygon rotatedBounds, final String unit, final double x,
                               final AffineTransform worldToScreenTransform) {
        Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
        LineString lineString = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(x, envelopeInternal.centre().y),
                new Coordinate(x, envelopeInternal.getMaxY())});
        Geometry intersections = lineString.intersection(rotatedBounds.getExteriorRing());

        if (intersections.getNumPoints() > 0) {
            Coordinate borderIntersection = intersections.getGeometryN(0).getCoordinates()[0];
            double[] screenPoints = new double[2];
            worldToScreenTransform.transform(new double[]{borderIntersection.x, borderIntersection.y}, 0, screenPoints, 0, 1);

            labels.add(new GridLabel(createLabel(x, unit), (int) screenPoints[0], (int) screenPoints[1], TOP));
        }
    }

    /**
     * Calculate the position and label of the bottom grid label and add it to the label collector.
     *
     * @param labels the label collector
     * @param geometryFactory the geometry factory for creating JTS geometries
     * @param rotatedBounds the full bounds of the map taking rotation into account.
     * @param unit the unit of the project, used to create label.
     * @param x the x coordinate where the grid line is.
     * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
     */
    public static void bottomBorderLabel(final LabelPositionCollector labels, final GeometryFactory geometryFactory,
                                  final Polygon rotatedBounds, final String unit, final double x,
                                  final AffineTransform worldToScreenTransform) {
        Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
        LineString lineString = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(x, envelopeInternal.getMinY()),
                new Coordinate(x, envelopeInternal.centre().y)});
        Geometry intersections = lineString.intersection(rotatedBounds.getExteriorRing());

        if (intersections.getNumPoints() > 0) {
            int idx = intersections instanceof LineString ? 1 : 0;
            Coordinate borderIntersection = intersections.getGeometryN(0).getCoordinates()[idx];
            double[] screenPoints = new double[2];
            worldToScreenTransform.transform(new double[]{borderIntersection.x, borderIntersection.y}, 0, screenPoints, 0, 1);

            labels.add(new GridLabel(createLabel(x, unit), (int) screenPoints[0], (int) screenPoints[1], BOTTOM));
        }
    }

    /**
     * Calculate the position and label of the right side grid label and add it to the label collector.
     *
     * @param labels the label collector
     * @param geometryFactory the geometry factory for creating JTS geometries
     * @param rotatedBounds the full bounds of the map taking rotation into account.
     * @param unit the unit of the project, used to create label.
     * @param y the y coordinate where the grid line is.
     * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
     */
    public static void rightBorderLabel(final LabelPositionCollector labels, final GeometryFactory geometryFactory,
                                 final Polygon rotatedBounds, final String unit, final double y,
                                 final AffineTransform worldToScreenTransform) {
        Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
        LineString lineString = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(envelopeInternal.centre().x, y),
                new Coordinate(envelopeInternal.getMaxX(), y)});
        Geometry intersections = lineString.intersection(rotatedBounds.getExteriorRing());

        if (intersections.getNumPoints() > 0) {
            int idx = intersections instanceof LineString ? 1 : 0;
            Coordinate borderIntersection = intersections.getGeometryN(0).getCoordinates()[idx];
            double[] screenPoints = new double[2];
            worldToScreenTransform.transform(new double[]{borderIntersection.x, borderIntersection.y}, 0, screenPoints, 0, 1);

            labels.add(new GridLabel(createLabel(y, unit), (int) screenPoints[0], (int) screenPoints[1], RIGHT));
        }
    }
    /**
     * Calculate the position and label of the left side grid label and add it to the label collector.
     *
     * @param labels the label collector
     * @param geometryFactory the geometry factory for creating JTS geometries
     * @param rotatedBounds the full bounds of the map taking rotation into account.
     * @param unit the unit of the project, used to create label.
     * @param y the y coordinate where the grid line is.
     * @param worldToScreenTransform the transform for mapping from world to screen(pixel)
     */
    static void leftBorderLabel(final LabelPositionCollector labels, final GeometryFactory geometryFactory,
                                final Polygon rotatedBounds, final String unit, final double y,
                                final AffineTransform worldToScreenTransform) {
        Envelope envelopeInternal = rotatedBounds.getEnvelopeInternal();
        LineString lineString = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(envelopeInternal.getMinX(), y),
                new Coordinate(envelopeInternal.centre().x, y)});
        Geometry intersections = lineString.intersection(rotatedBounds.getExteriorRing());

        if (intersections.getNumPoints() > 0) {
            double[] screenPoints = new double[2];
            Coordinate borderIntersection = intersections.getGeometryN(0).getCoordinates()[0];
            worldToScreenTransform.transform(new double[]{borderIntersection.x, borderIntersection.y}, 0, screenPoints, 0, 1);

            labels.add(new GridLabel(createLabel(y, unit), (int) screenPoints[0], (int) screenPoints[1], LEFT));
        }
    }
}
