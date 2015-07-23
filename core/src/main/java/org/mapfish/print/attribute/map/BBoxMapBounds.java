/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.attribute.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;

/**
 * Represent the map bounds with a bounding box.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public final class BBoxMapBounds extends MapBounds {
    private final Envelope bbox;

    /**
     * Constructor.
     *
     * @param projection the projection these bounds are defined in.
     * @param envelope   the bounds
     */
    public BBoxMapBounds(final CoordinateReferenceSystem projection, final Envelope envelope) {
        super(projection);
        this.bbox = envelope;
    }

    /**
     * Constructor.
     *
     * @param projection the projection these bounds are defined in.
     * @param minX       min X coordinate for the MapBounds
     * @param minY       min Y coordinate for the MapBounds
     * @param maxX       max X coordinate for the MapBounds
     * @param maxY       max Y coordinate for the MapBounds
     */
    public BBoxMapBounds(final CoordinateReferenceSystem projection, final double minX, final double minY,
                         final double maxX, final double maxY) {
        this(projection, new Envelope(minX, maxX, minY, maxY));
    }

    /**
     * Create from a bbox.
     * @param bbox the bounds.
     */
    public BBoxMapBounds(final ReferencedEnvelope bbox) {
        this(bbox.getCoordinateReferenceSystem(), bbox.getMinX(), bbox.getMinY(),
                bbox.getMaxX(), bbox.getMaxY());

    }

    @Override
    public ReferencedEnvelope toReferencedEnvelope(final Rectangle paintArea, final double dpi) {
        return new ReferencedEnvelope(this.bbox, getProjection());
    }

    @Override
    public MapBounds adjustedEnvelope(final Rectangle paintArea) {
        final double paintAreaAspectRatio = paintArea.getWidth() / paintArea.getHeight();
        final double bboxAspectRatio = this.bbox.getWidth() / this.bbox.getHeight();
        if (paintAreaAspectRatio > bboxAspectRatio) {
            double centerX = (this.bbox.getMinX() + this.bbox.getMaxX()) / 2;
            double factor = paintAreaAspectRatio / bboxAspectRatio;
            double finalDiff =  (this.bbox.getMaxX() - centerX) * factor;

            return new BBoxMapBounds(getProjection(),
                    centerX - finalDiff, this.bbox.getMinY(),
                    centerX + finalDiff, this.bbox.getMaxY());
        } else {
            double centerY = (this.bbox.getMinY() + this.bbox.getMaxY()) / 2;
            double factor = bboxAspectRatio / paintAreaAspectRatio;
            double finalDiff =  (this.bbox.getMaxY() - centerY) * factor;
            return new BBoxMapBounds(getProjection(),
                    this.bbox.getMinX(), centerY - finalDiff,
                    this.bbox.getMaxX(), centerY + finalDiff);
        }
    }

    @Override
    public MapBounds adjustBoundsToNearestScale(final ZoomLevels zoomLevels, final double tolerance,
                                                final ZoomLevelSnapStrategy zoomLevelSnapStrategy, final Rectangle paintArea,
                                                final double dpi) {
        final Scale scale = getScaleDenominator(paintArea, dpi);
        final ZoomLevelSnapStrategy.SearchResult result = zoomLevelSnapStrategy.search(scale, tolerance, zoomLevels);
        Coordinate center = this.bbox.centre();


        return new CenterScaleMapBounds(getProjection(), center.x, center.y, result.getScale());

    }

    @Override
    public Scale getScaleDenominator(final Rectangle paintArea, final double dpi) {
        final ReferencedEnvelope bboxAdjustedToScreen = toReferencedEnvelope(paintArea, dpi);

        DistanceUnit projUnit = DistanceUnit.fromProjection(getProjection());

        double geoWidthInInches;
        if (projUnit == DistanceUnit.DEGREES) {
            GeodeticCalculator calculator = new GeodeticCalculator(getProjection());
            final double centerY = bboxAdjustedToScreen.centre().y;
            calculator.setStartingGeographicPoint(bboxAdjustedToScreen.getMinX(), centerY);
            calculator.setDestinationGeographicPoint(bboxAdjustedToScreen.getMaxX(), centerY);
            double geoWidthInEllipsoidUnits = calculator.getOrthodromicDistance();
            DistanceUnit ellipsoidUnit = DistanceUnit.fromString(calculator.getEllipsoid().getAxisUnit().toString());

            geoWidthInInches = ellipsoidUnit.convertTo(geoWidthInEllipsoidUnits, DistanceUnit.IN);
        } else {
            // (scale * width ) / dpi = geowidith
            geoWidthInInches = projUnit.convertTo(bboxAdjustedToScreen.getWidth(), DistanceUnit.IN);
        }

        double scale = geoWidthInInches * (dpi / paintArea.getWidth());
        return new Scale(scale);
    }

    @Override
    public MapBounds adjustBoundsToRotation(final double rotation) {
        if (rotation == 0.0) {
            return this;
        }

        /**
         * When a rotation is set, the map is rotated around the center of the
         * original bbox. This means that the bbox might has to be expanded, so
         * that all visible parts are rendered.
         */
        final double rotatedWidth = getRotatedWidth(rotation);
        final double rotatedHeight = getRotatedHeight(rotation);

        final double widthDifference = (rotatedWidth - this.bbox.getWidth()) / 2.0;
        final double rotatedMinX = this.bbox.getMinX() - widthDifference;
        final double rotatedMaxX = this.bbox.getMaxX() + widthDifference;

        final double heightDifference = (rotatedHeight - this.bbox.getHeight()) / 2.0;
        final double rotatedMinY = this.bbox.getMinY() - heightDifference;
        final double rotatedMaxY = this.bbox.getMaxY() + heightDifference;

        return new BBoxMapBounds(getProjection(),
                rotatedMinX, rotatedMinY, rotatedMaxX, rotatedMaxY);
    }

    private double getRotatedWidth(final double rotation) {
        double width = this.bbox.getWidth();
        if (rotation != 0.0) {
            double height = this.bbox.getHeight();
            width = (float) (Math.abs(width * Math.cos(rotation)) +
                    Math.abs(height * Math.sin(rotation)));
        }
        return width;
    }

    private double getRotatedHeight(final double rotation) {
        double height = this.bbox.getHeight();
        if (rotation != 0.0) {
            double width = this.bbox.getWidth();
            height = (float) (Math.abs(height * Math.cos(rotation)) +
                    Math.abs(width * Math.sin(rotation)));
        }
        return height;
    }

    @Override
    public MapBounds zoomOut(final double factor) {
        if (factor == 1.0) {
            return this;
        }

        double destWidth = this.bbox.getWidth() * factor;
        double destHeight = this.bbox.getHeight() * factor;

        double centerX = this.bbox.centre().x;
        double centerY = this.bbox.centre().y;

        double minGeoX = centerX - destWidth / 2.0f;
        double maxGeoX = centerX + destWidth / 2.0f;
        double minGeoY = centerY - destHeight / 2.0f;
        double maxGeoY = centerY + destHeight / 2.0f;

        return new BBoxMapBounds(getProjection(),
                minGeoX, minGeoY, maxGeoX, maxGeoY);
    }

    @Override
    public MapBounds zoomToScale(final double scale) {
        Coordinate center = this.bbox.centre();
        return new CenterScaleMapBounds(getProjection(), center.x, center.y, new Scale(scale));
    }

    /**
     * Expand the bounds by the given margin (in pixel).
     *
     * @param margin Value in pixel.
     * @param paintArea the paint area of the map.
     */
    public MapBounds expand(final int margin, final Rectangle paintArea) {
        double factorX = 1.0 + (2 * margin) / paintArea.getWidth();
        double factorY = 1.0 + (2 * margin) / paintArea.getHeight();

        double destWidth = this.bbox.getWidth() * factorX;
        double destHeight = this.bbox.getHeight() * factorY;

        double centerX = this.bbox.centre().x;
        double centerY = this.bbox.centre().y;

        double minGeoX = centerX - destWidth / 2.0;
        double maxGeoX = centerX + destWidth / 2.0;
        double minGeoY = centerY - destHeight / 2.0;
        double maxGeoY = centerY + destHeight / 2.0;

        return new BBoxMapBounds(getProjection(),
                minGeoX, minGeoY, maxGeoX, maxGeoY);
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
        if (!super.equals(o)) {
            return false;
        }

        BBoxMapBounds that = (BBoxMapBounds) o;

        return bbox.equals(that.bbox);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + bbox.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BBoxMapBounds{" +
               "bbox=" + bbox +
               '}';
    }
// // CHECKSTYLE:ON

}
