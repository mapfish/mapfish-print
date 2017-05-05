package org.mapfish.print.map;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.attribute.map.GenericMapAttribute;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Represent a scale and provide transformation.
 */
public final class Scale {
    private final double resolution;
    private static final Logger LOGGER = LoggerFactory.getLogger(Scale.class);

    /**
     * Constructor.
     *
     * @param denominator the scale denominator.  a value of 1'000 would be a scale of 1:1'000.
     * @param projection the projection.
     * @param dpi the DPI on witch the scale is valid.
     */
    public Scale(final double denominator, @Nonnull final CoordinateReferenceSystem projection, final double dpi) {
        this(denominator, DistanceUnit.fromProjection(projection), dpi);
    }

    /**
     * Constructor.
     *
     * @param denominator the scale denominator.  a value of 1'000 would be a scale of 1:1'000.
     * @param projectionUnit the unit used by the projection.
     * @param dpi the DPI on witch the scale is valid.
     */
    public Scale(final double denominator, @Nonnull final DistanceUnit projectionUnit, final double dpi) {
        this(1.0 / (projectionUnit.convertTo(1.0 / denominator, DistanceUnit.IN) * dpi));
    }

    /**
     * Constructor.
     *
     * @param resolution the resolution.
     */
    private Scale(final double resolution) {
        this.resolution = resolution;
    }

    /**
     * Get the resolution.
     * @return the resolution
     */
    public double getResolution() {
        return this.resolution;
    }

    /**
     * @param geodetic geodetic mode.
     * @param projection the projection to perform the calculation in.
     * @param dpi the dpi of the display device.
     * @param position the position on the map.
     * @return the scale denominator
     */
    public double getDenominator(
            final boolean geodetic, @Nonnull final CoordinateReferenceSystem projection,
            final double dpi, final Coordinate position) {
        return geodetic ?
                getGeodeticDenominator(projection, dpi, position) :
                getDenominator(projection, dpi);
    }

    /**
     * @param projection the projection to perform the calculation in
     * @param dpi the dpi of the display device.
     * @return the scale denominator
     */
    public double getDenominator(@Nonnull final CoordinateReferenceSystem projection, final double dpi) {
        return getDenominator(DistanceUnit.fromProjection(projection), dpi);
    }

    /**
     * @param projectionUnit the projection unit
     * @param dpi the dpi of the display device.
     * @return the scale denominator
     */
    public double getDenominator(@Nonnull final DistanceUnit projectionUnit, final double dpi) {
        final double resolutionInInches = projectionUnit.convertTo(this.resolution, DistanceUnit.IN);
        return resolutionInInches * dpi;
    }

    /**
     * @param projection the projection to perform the calculation in
     * @param dpi the dpi of the display device.
     * @param position the position on the map.
     * @return the scale denominator
     */
    public double getGeodeticDenominator(@Nonnull final CoordinateReferenceSystem projection, final double dpi, final Coordinate position) {
        final DistanceUnit projectionUnit = DistanceUnit.fromProjection(projection);
        double scaleDenominator = getDenominator(projectionUnit, dpi);

        if (projectionUnit == DistanceUnit.DEGREES) {
            return scaleDenominator;
        }

        try {
            double width = 1;
            double geoWidthInches = scaleDenominator * width / dpi;
            double geoWidth = DistanceUnit.IN.convertTo(geoWidthInches, projectionUnit);
            double minGeoX = position.y - (geoWidth / 2.0);
            double maxGeoX = minGeoX + geoWidth;

            final GeodeticCalculator calculator = new GeodeticCalculator(projection);
            final double centerY = position.y;

            final MathTransform transform = CRS.findMathTransform(projection,
                    GenericMapAttribute.parseProjection("EPSG:4326", true));
            final Coordinate start = JTS.transform(new Coordinate(minGeoX, centerY), null, transform);
            final Coordinate end = JTS.transform(new Coordinate(maxGeoX, centerY), null, transform);
            calculator.setStartingGeographicPoint(start.x, start.y);
            calculator.setDestinationGeographicPoint(end.x, end.y);
            final double geoWidthInEllipsoidUnits = calculator.getOrthodromicDistance();
            final DistanceUnit ellipsoidUnit = DistanceUnit.fromString(calculator.getEllipsoid().getAxisUnit().toString());

            final double geoWidthInInches = ellipsoidUnit.convertTo(geoWidthInEllipsoidUnits, DistanceUnit.IN);
            return geoWidthInInches * (dpi / width);
        } catch (FactoryException e) {
            LOGGER.error("Unable to do the geodetic calculation on the scale", e);
        } catch (TransformException e) {
            LOGGER.error("Unable to do the geodetic calculation on the scale", e);
        }

        // fall back
        return getDenominator(projectionUnit, dpi);
    }

    /**
     * @param geodetic Do in geodetic.
     * @param scaleDenominator the scale denominator.
     * @param projection the projection to perform the calculation in.
     * @param dpi the dpi of the display device.
     * @param position the position on the map.
     * @return the scale denominator.
     */
    public static double getDenominator(
            final boolean geodetic,
            final double scaleDenominator, @Nonnull final CoordinateReferenceSystem projection,
            final double dpi, final Coordinate position) {
        return geodetic ? getGeodeticDenominator(scaleDenominator, projection, dpi, position) : scaleDenominator;
    }

    /**
     * @param scaleDenominator the scale denominator.
     * @param projection the projection to perform the calculation in.
     * @param dpi the dpi of the display device.
     * @param position the position on the map.
     * @return the scale denominator.
     */
    public static double getGeodeticDenominator(
            final double scaleDenominator, @Nonnull final CoordinateReferenceSystem projection,
            final double dpi, final Coordinate position) {
        return new Scale(scaleDenominator, DistanceUnit.fromProjection(projection), dpi).getGeodeticDenominator(projection, dpi, position);
    }

    /**
     * Construct a scale object from a resolution.
     *
     * @param resolution the resolution of the map
     */
    public static Scale fromResolution(final double resolution) {
        return new Scale(resolution);
    }

    // CHECKSTYLE:OFF

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Scale scale = (Scale) o;

        if (Double.compare(scale.resolution, resolution) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new Double(resolution).hashCode();
    }

    @Override
    public String toString() {
        return "Scale{resolution=" + resolution + '}';
    }
    // CHECKSTYLE:ON
}
