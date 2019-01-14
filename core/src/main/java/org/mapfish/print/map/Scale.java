package org.mapfish.print.map;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.mapfish.print.attribute.map.GenericMapAttribute;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static org.mapfish.print.Constants.PDF_DPI;

/**
 * Represent a scale and provide transformation.
 */
public final class Scale implements Comparable<Scale> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scale.class);
    private final double resolution;
    private final DistanceUnit unit;

    /**
     * Constructor.
     *
     * @param denominator the scale denominator.  a value of 1'000 would be a scale of 1:1'000.
     * @param projection the projection.
     * @param dpi the DPI on witch the scale is valid.
     */
    public Scale(
            final double denominator,
            @Nonnull final CoordinateReferenceSystem projection,
            final double dpi) {
        this(denominator, DistanceUnit.fromProjection(projection), dpi);
    }

    /**
     * Constructor.
     *
     * @param denominator the scale denominator.  a value of 1'000 would be a scale of 1:1'000.
     * @param unit the unit used by the projection.
     * @param dpi the DPI on witch the scale is valid.
     */
    public Scale(final double denominator, @Nonnull final DistanceUnit unit, final double dpi) {
        this(
                1.0 / (unit.convertTo(1.0 / denominator, DistanceUnit.IN) * dpi),
                unit);
    }

    /**
     * Constructor.
     *
     * @param resolution the resolution.
     * @param unit the unit used by the projection.
     */
    private Scale(final double resolution, @Nonnull final DistanceUnit unit) {
        this.resolution = resolution;
        this.unit = unit;
    }

    /**
     * Constructor.
     *
     * @param resolution the resolution.
     * @param projection the projection.
     */
    private Scale(final double resolution, @Nonnull final CoordinateReferenceSystem projection) {
        this.resolution = resolution;
        this.unit = DistanceUnit.fromProjection(projection);
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
        return geodetic ? getGeodeticDenominator(scaleDenominator, projection, dpi, position) :
                scaleDenominator;
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
        return new Scale(scaleDenominator, DistanceUnit.fromProjection(projection), dpi)
                .getGeodeticDenominator(projection, dpi, position);
    }

    /**
     * Construct a scale object from a resolution.
     *
     * @param resolution the resolution of the map
     * @param projectionUnit the unit used by the projection.
     */
    public static Scale fromResolution(
            final double resolution, @Nonnull final DistanceUnit projectionUnit) {
        return new Scale(resolution, projectionUnit);
    }

    /**
     * Construct a scale object from a resolution.
     *
     * @param resolution the resolution of the map
     * @param projection the projection to perform the calculation in.
     */
    public static Scale fromResolution(
            final double resolution, @Nonnull final CoordinateReferenceSystem projection) {
        return new Scale(resolution, projection);
    }

    /**
     * Get the resolution in meters.
     *
     * @return the resolution
     */
    public double getResolution() {
        return this.resolution;
    }

    /**
     * Get the resolution in inches.
     *
     * @return the resolution
     */
    public double getResolutionInInches() {
        return this.unit.convertTo(this.resolution, DistanceUnit.IN);
    }

    /**
     * Get the scale unit.
     *
     * @return the unit
     */
    public DistanceUnit getUnit() {
        return this.unit;
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
                getDenominator(dpi);
    }

    /**
     * @param dpi the dpi of the display device.
     * @return the scale denominator
     */
    public double getDenominator(final double dpi) {
        final double resolutionInInches = getResolutionInInches();
        return resolutionInInches * dpi;
    }

    /**
     * @return the scale denominator in the PDF resolution
     */
    public double getDenominator() {
        return getDenominator(PDF_DPI);
    }

    /**
     * @param projection the projection to perform the calculation in.
     * @param dpi the dpi of the display device.
     * @param position the position on the map.
     * @return the scale denominator
     */
    public double getGeodeticDenominator(
            @Nonnull final CoordinateReferenceSystem projection, final double dpi,
            final Coordinate position) {
        if (this.unit == DistanceUnit.DEGREES) {
            return getDenominator(dpi);
        }

        try {
            double width = 1;
            double geoWidthInches = getResolutionInInches() * width;
            double geoWidth = DistanceUnit.IN.convertTo(geoWidthInches, this.unit);
            double minGeoX = position.y - (geoWidth / 2.0);
            double maxGeoX = minGeoX + geoWidth;

            final GeodeticCalculator calculator = new GeodeticCalculator(projection);
            final double centerY = position.y;

            final MathTransform transform = CRS.findMathTransform(projection,
                                                                  GenericMapAttribute
                                                                          .parseProjection("EPSG:4326",
                                                                                           true));
            final Coordinate start = JTS.transform(new Coordinate(minGeoX, centerY), null, transform);
            final Coordinate end = JTS.transform(new Coordinate(maxGeoX, centerY), null, transform);
            calculator.setStartingGeographicPoint(start.x, start.y);
            calculator.setDestinationGeographicPoint(end.x, end.y);
            final double geoWidthInEllipsoidUnits = calculator.getOrthodromicDistance();
            final DistanceUnit ellipsoidUnit =
                    DistanceUnit.fromString(calculator.getEllipsoid().getAxisUnit().toString());

            final double geoWidthInInches =
                    ellipsoidUnit.convertTo(geoWidthInEllipsoidUnits, DistanceUnit.IN);
            return geoWidthInInches * (dpi / width);
        } catch (FactoryException | TransformException e) {
            LOGGER.error("Unable to do the geodetic calculation on the scale", e);
        }

        // fall back
        return getDenominator(dpi);
    }

    /**
     * Construct a scale object from a resolution.
     *
     * @param newResolution the resolution of the map
     */
    public Scale toResolution(final double newResolution) {
        return Scale.fromResolution(newResolution, this.unit);
    }

    @Override
    public int compareTo(final Scale scale) {
        if (!this.unit.equals(scale.unit)) {
            throw new RuntimeException("Unable to compare scales in different units");
        }
        return Double.compare(this.resolution, scale.resolution);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Scale scale = (Scale) o;

        if (Double.compare(scale.resolution, this.resolution) != 0) {
            return false;
        }
        return this.unit == scale.unit;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(this.resolution);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (this.unit != null ? this.unit.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Scale{resolution=%s}", this.resolution);
    }
}
