package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.geom.AffineTransform;
import javax.annotation.Nonnull;

/**
 * @author Jesse on 7/10/2015.
 */
public final class GridUtils {
    private GridUtils() {
        // do nothing
    }

    static Polygon calculateBounds(final MapfishMapContext context) {
        double rotation = context.getRotation();
        ReferencedEnvelope env = context.toReferencedEnvelope();

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

    static double calculateFirstLine(final ReferencedEnvelope bounds,
                                     final GridParam layerData,
                                     final int ordinal) {
        return calculateFirstLine(bounds, layerData, ordinal, 0);
    }

    static double calculateFirstLine(final ReferencedEnvelope bounds,
                                     final GridParam layerData,
                                     final int ordinal,
                                     final int indent) {
        double spaceFromOrigin = bounds.getMinimum(ordinal) + indent - layerData.origin[ordinal];
        double linesBetweenOriginAndMap = Math.ceil(spaceFromOrigin / layerData.spacing[ordinal]);

        return linesBetweenOriginAndMap * layerData.spacing[ordinal] + layerData.origin[ordinal];
    }

    /**
     * Create the grid feature type.
     *
     * @param mapContext the map context containing the information about the map the grid will be added to.
     * @param geomClass  the geometry type
     */
    static SimpleFeatureType createGridFeatureType(@Nonnull final MapfishMapContext mapContext,
                                                   @Nonnull final Class<? extends Geometry> geomClass) {
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        CoordinateReferenceSystem projection = mapContext.getBounds().getProjection();
        typeBuilder.add(Constants.Style.Grid.ATT_GEOM, geomClass, projection);
        typeBuilder.add(Constants.Style.Grid.ATT_LABEL, String.class);
        typeBuilder.add(Constants.Style.Grid.ATT_ROTATION, Double.class);
        typeBuilder.add(Constants.Style.Grid.ATT_X_DISPLACEMENT, Double.class);
        typeBuilder.add(Constants.Style.Grid.ATT_Y_DISPLACEMENT, Double.class);
        typeBuilder.add(Constants.Style.Grid.ATT_ANCHOR_X, Double.class);
        typeBuilder.setName(Constants.Style.Grid.NAME_LINES);

        return typeBuilder.buildFeatureType();
    }

    /**
     * Create the label for the a grid line.
     *
     * @param value the value of the line
     * @param unit  the unit that the value is in
     */
    static String createLabel(final double value, final String unit) {
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
                return String.format("%f1.2 %s", value, unit);
            } else if (value > minBeforeScientific) {
                return String.format("%1.4f %s", value, unit);
            } else {
                return String.format("%e %s", value, unit);
            }
        }
    }
}
