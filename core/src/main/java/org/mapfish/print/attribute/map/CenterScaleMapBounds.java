package org.mapfish.print.attribute.map;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;

/**
 * Represent Map Bounds with a center location and a scale of the map.
 * <p></p>
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
     * @param scale   the scale of the map
     */
    public CenterScaleMapBounds(final CoordinateReferenceSystem projection, final double centerX,
                                final double centerY, final Scale scale) {
        super(projection);
        this.center = new Coordinate(centerX, centerY);
        this.scale = scale;
    }


    @Override
    public ReferencedEnvelope toReferencedEnvelope(final Rectangle paintArea, final double dpi) {

        double geoWidthInches = this.scale.getDenominator() * paintArea.width / dpi;
        double geoHeightInches = this.scale.getDenominator() * paintArea.height / dpi;

        ReferencedEnvelope bbox;

        final DistanceUnit projectionUnit = DistanceUnit.fromProjection(getProjection());
        if (projectionUnit == DistanceUnit.DEGREES) {
            bbox = computeGeodeticBBox(geoWidthInches, geoHeightInches);
        } else {
            final double centerX = this.center.getOrdinate(0);
            final double centerY = this.center.getOrdinate(1);

            double geoWidth = DistanceUnit.IN.convertTo(geoWidthInches, projectionUnit);
            double geoHeight = DistanceUnit.IN.convertTo(geoHeightInches, projectionUnit);

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
            final Rectangle paintArea, final double dpi) {

        Scale resultScale;
        if (geodetic) {
            final Scale currentScale = getScaleDenominator(paintArea, dpi);
            final Scale geodeticScale = getGeodeticScaleDenominator(paintArea, dpi);
            final ZoomLevelSnapStrategy.SearchResult result = zoomLevelSnapStrategy.search(
                    geodeticScale, tolerance, zoomLevels);
            resultScale = new Scale(result.getScale().getDenominator() *
                    currentScale.getDenominator() / geodeticScale.getDenominator());
        } else {
            final ZoomLevelSnapStrategy.SearchResult result = zoomLevelSnapStrategy.search(this.scale, tolerance, zoomLevels);
            resultScale = result.getScale();
        }

        return new CenterScaleMapBounds(getProjection(), this.center.x, this.center.y, resultScale);
    }

    @Override
    public Scale getScaleDenominator(final Rectangle paintArea, final double dpi) {
        return this.scale;
    }

    @Override
    public MapBounds adjustBoundsToRotation(final double rotation) {
        // nothing to change when rotating, because the center stays the same
        return this;
    }

    @Override
    public CenterScaleMapBounds zoomOut(final double factor) {
        if (factor == 1.0) {
            return this;
        }

        final double newDenominator = this.scale.getDenominator() * factor;
        return new CenterScaleMapBounds(getProjection(), this.center.x, this.center.y, new Scale(newDenominator));
    }

    @Override
    public MapBounds zoomToScale(final double newScale) {
        return new CenterScaleMapBounds(getProjection(), this.center.x, this.center.y, new Scale(newScale));
    }

    private ReferencedEnvelope computeGeodeticBBox(final double geoWidthInInches, final double geoHeightInInches) {
        try {

            CoordinateReferenceSystem crs = getProjection();

            GeodeticCalculator calc = new GeodeticCalculator(crs);

            DistanceUnit ellipsoidUnit = DistanceUnit.fromString(calc.getEllipsoid().getAxisUnit().toString());
            double geoWidth = DistanceUnit.IN.convertTo(geoWidthInInches, ellipsoidUnit);
            double geoHeight = DistanceUnit.IN.convertTo(geoHeightInInches, ellipsoidUnit);

            DirectPosition2D directPosition2D = new DirectPosition2D(this.center.x, this.center.y);
            directPosition2D.setCoordinateReferenceSystem(crs);
            calc.setStartingPosition(directPosition2D);

            final int west = -90;
            calc.setDirection(west, geoWidth / 2.0);
            double minGeoX =  calc.getDestinationPosition().getOrdinate(0);

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

    // CSOFF: MagicNumber
    private double rollLongitude(final double x) {
        return x - (((int) (x + Math.signum(x) * 180)) / 360) * 360.0;
    }

    private double rollLatitude(final double y) {
        return y - (((int) (y + Math.signum(y) * 90)) / 180) * 180.0;
    }
    // CSON: MagicNumber

    // CHECKSTYLE:OFF

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CenterScaleMapBounds that = (CenterScaleMapBounds) o;

        if (!center.equals(that.center)) {
            return false;
        }
        if (!scale.equals(that.scale)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + center.hashCode();
        result = 31 * result + scale.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CenterScaleMapBounds{" +
               "center=" + center +
               ", scale=" + scale +
               '}';
    }
    // CHECKSTYLE:ON
}
