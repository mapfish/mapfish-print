package org.mapfish.print.attribute.map;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.FloatingPointUtil;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;

import static org.mapfish.print.Constants.PDF_DPI;

/**
 * Represent Map Bounds with a center location and a scale of the map.
 *
 * Created by Jesse on 3/26/14.
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
            final CoordinateReferenceSystem projection, final double centerX,
            final double centerY, final double scaleDenominator) {
        super(projection);
        this.center = new Coordinate(centerX, centerY);
        this.scale = new Scale(scaleDenominator, getProjection(), PDF_DPI);
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
            final CoordinateReferenceSystem projection, final double centerX,
            final double centerY, final Scale scale) {
        super(projection);
        this.center = new Coordinate(centerX, centerY);
        this.scale = scale;
    }

    @Override
    public ReferencedEnvelope toReferencedEnvelope(final Rectangle paintArea) {
        ReferencedEnvelope bbox;
        final DistanceUnit projectionUnit = DistanceUnit.fromProjection(getProjection());
        if (projectionUnit == DistanceUnit.DEGREES) {
            double geoWidthInches = this.scale.getResolutionInInches() * paintArea.width;
            double geoHeightInches = this.scale.getResolutionInInches() * paintArea.height;
            bbox = computeGeodeticBBox(geoWidthInches, geoHeightInches);
        } else {
            final double centerX = this.center.getOrdinate(0);
            final double centerY = this.center.getOrdinate(1);

            double geoWidth = this.scale.getResolution() * paintArea.width;
            double geoHeight = this.scale.getResolution() * paintArea.height;

            double minGeoX = centerX - (geoWidth / 2.0);
            double minGeoY = centerY - (geoHeight / 2.0);
            double maxGeoX = minGeoX + geoWidth;
            double maxGeoY = minGeoY + geoHeight;
            bbox = new ReferencedEnvelope(minGeoX, maxGeoX, minGeoY, maxGeoY, getProjection());
        }

        return bbox;
    }

    @Override
    public MapBounds adjustedEnvelope(final Rectangle paintArea) {
        return this;
    }

    @Override
    public MapBounds adjustBoundsToNearestScale(
            final ZoomLevels zoomLevels, final double tolerance,
            final ZoomLevelSnapStrategy zoomLevelSnapStrategy,
            final boolean geodetic,
            final Rectangle paintArea,
            final double dpi) {

        final Scale newScale = getNearestScale(zoomLevels, tolerance, zoomLevelSnapStrategy,
                                               geodetic, paintArea, dpi);

        return new CenterScaleMapBounds(getProjection(), this.center.x, this.center.y,
                                        newScale);
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
        return new CenterScaleMapBounds(getProjection(), this.center.x, this.center.y,
                                        this.scale.toResolution(newResolution));
    }

    @Override
    public MapBounds zoomToScale(final Scale newScale) {
        return new CenterScaleMapBounds(getProjection(), this.center.x, this.center.y, newScale);
    }

    @Override
    public Coordinate getCenter() {
        return this.center;
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

            DirectPosition2D directPosition2D = new DirectPosition2D(this.center.x, this.center.y);
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
                    rollLongitude(minGeoX), rollLongitude(maxGeoX),
                    rollLatitude(minGeoY), rollLatitude(maxGeoY), crs);
        } catch (TransformException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    private double rollLongitude(final double x) {
        return x - (((int) (x + Math.signum(x) * 180)) / 360) * 360.0;
    }

    private double rollLatitude(final double y) {
        return y - (((int) (y + Math.signum(y) * 90)) / 180) * 180.0;
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

        if (this.center != null ? !this.center.equals(that.center) : that.center != null) {
            return false;
        }
        return this.scale != null ? this.scale.equals(that.scale) : that.scale == null;
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
