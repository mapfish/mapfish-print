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

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class Represents the bounds of the map in some way.  The implementations will represent the as a bbox or as a center and scale.
 * Created by Jesse on 3/26/14.
 */
public abstract class MapBounds {
    private final CoordinateReferenceSystem projection;
    private static final Logger LOGGER = LoggerFactory.getLogger(MapBounds.class);

    /**
     * Constructor.
     *
     * @param projection the projection these bounds are defined in.
     */
    protected MapBounds(final CoordinateReferenceSystem projection) {
        this.projection = projection;
    }

    /**
     * Create a {@link org.geotools.geometry.jts.ReferencedEnvelope} representing the bounds.
     * <p/>
     *
     * @param paintArea the size of the map that will be drawn.
     * @param dpi the dpi of the map
     */
    public abstract ReferencedEnvelope toReferencedEnvelope(Rectangle paintArea, double dpi);

    /**
     * Create a {@link org.geotools.geometry.jts.ReferencedEnvelope} representing the bounds.
     * <p/>
     *
     * @param paintArea the size of the map that will be drawn.
     */
    public abstract MapBounds adjustedEnvelope(Rectangle paintArea);

    /**
     * Get the projection these bounds are calculated in.
     */
    public final CoordinateReferenceSystem getProjection() {
        return this.projection;
    }

    /**
     * Adjust these bounds so that they are adjusted to the nearest scale in the provided set of scales.
     *
     * The center should remain the same and the scale should be adjusted
     *
     * @param zoomLevels the list of Zoom Levels
     * @param tolerance the tolerance to use when considering if two values are equal.  For example if 12.0 == 12.001.
     *                  The tolerance is a percentage
     * @param zoomLevelSnapStrategy the strategy to use for snapping to the nearest zoom level.
     * @param geodetic snap to geodetic scales.
     * @param paintArea the paint area of the map.
     * @param dpi the dpi of the map
     */
    public abstract MapBounds adjustBoundsToNearestScale(
            final ZoomLevels zoomLevels, final double tolerance,
            final ZoomLevelSnapStrategy zoomLevelSnapStrategy,
            final boolean geodetic,
            final Rectangle paintArea, final double dpi);

    /**
     * Calculate and return the scale of the map bounds.
     *
     * @param paintArea the paint area of the map.
     * @param dpi the dpi of the map
     */
    public abstract Scale getScaleDenominator(final Rectangle paintArea, final double dpi);

    /**
     * Calculate and return the geodetic scale of the map bounds.
     *
     * @param paintArea the paint area of the map.
     * @param dpi the dpi of the map
     */
    public final Scale getGeodeticScaleDenominator(final Rectangle paintArea, final double dpi) {

        DistanceUnit projUnit = DistanceUnit.fromProjection(getProjection());

        if (projUnit == DistanceUnit.DEGREES) {
            return getScaleDenominator(paintArea, dpi);
        }

        try {
            final ReferencedEnvelope bboxAdjustedToScreen = toReferencedEnvelope(paintArea, dpi);

            final GeodeticCalculator calculator = new GeodeticCalculator(getProjection());
            final double centerY = bboxAdjustedToScreen.centre().y;

            final MathTransform transform = CRS.findMathTransform(getProjection(),
                    GenericMapAttribute.parseProjection("EPSG:4326", true));
            final Coordinate start = JTS.transform(new Coordinate(bboxAdjustedToScreen.getMinX(), centerY), null, transform);
            final Coordinate end = JTS.transform(new Coordinate(bboxAdjustedToScreen.getMaxX(), centerY), null, transform);
            calculator.setStartingGeographicPoint(start.x, start.y);
            calculator.setDestinationGeographicPoint(end.x, end.y);
            final double geoWidthInEllipsoidUnits = calculator.getOrthodromicDistance();
            final DistanceUnit ellipsoidUnit = DistanceUnit.fromString(calculator.getEllipsoid().getAxisUnit().toString());

            final double geoWidthInInches = ellipsoidUnit.convertTo(geoWidthInEllipsoidUnits, DistanceUnit.IN);
            return new Scale(geoWidthInInches * (dpi / paintArea.getWidth()));
        } catch (FactoryException e) {
            LOGGER.error("Unable to do the geodetic calculation on the scale", e);
        } catch (TransformException e) {
            LOGGER.error("Unable to do the geodetic calculation on the scale", e);
        }

        // fall back
        return getScaleDenominator(paintArea, dpi);
    }


    /**
     * In case a rotation is used for the map, the bounds have to be adjusted so that all
     * visible parts are rendered.
     *
     * @param rotation The rotation of the map in radians.
     * @return Bounds adjusted to the map rotation.
     */
    public abstract MapBounds adjustBoundsToRotation(final double rotation);

    /**
     * Zooms-out the bounds by the given factor.
     *
     * @param factor The zoom factor.
     * @return Bounds adjusted to the zoom factor.
     */
    public abstract MapBounds zoomOut(final double factor);

    /**
     * Zoom to the given scale.
     *
     * @param scale The new scale.
     * @return Bounds adjusted to the scale.
     */
    public abstract MapBounds zoomToScale(final double scale);

    // CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MapBounds mapBounds = (MapBounds) o;

        if (!projection.equals(mapBounds.projection)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return projection.hashCode();
    }
    // CHECKSTYLE:ON
}
