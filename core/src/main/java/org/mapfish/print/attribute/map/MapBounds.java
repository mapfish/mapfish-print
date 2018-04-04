package org.mapfish.print.attribute.map;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

import static org.mapfish.print.Constants.PDF_DPI;

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
     * <p></p>
     *
     * @param paintArea the size of the map that will be drawn (at 72 DPI).
     */
    public abstract ReferencedEnvelope toReferencedEnvelope(Rectangle paintArea);

    /**
     * Create a {@link org.geotools.geometry.jts.ReferencedEnvelope} representing the bounds.
     * <p></p>
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
     * @param dpi the DPI.
     */
    public abstract MapBounds adjustBoundsToNearestScale(
            final ZoomLevels zoomLevels,
            final double tolerance,
            final ZoomLevelSnapStrategy zoomLevelSnapStrategy,
            final boolean geodetic,
            final Rectangle paintArea,
            final double dpi);

    /**
     * Get the nearest scale.
     *
     * @param zoomLevels the list of Zoom Levels.
     * @param tolerance the tolerance to use when considering if two values are equal.
     *          For example if 12.0 == 12.001.
     *          The tolerance is a percentage.
     * @param zoomLevelSnapStrategy the strategy to use for snapping to the nearest zoom level.
     * @param geodetic snap to geodetic scales.
     * @param paintArea the paint area of the map.
     * @param dpi the DPI.
     */
    public Scale getNearestScale(
            final ZoomLevels zoomLevels,
            final double tolerance,
            final ZoomLevelSnapStrategy zoomLevelSnapStrategy,
            final boolean geodetic,
            final Rectangle paintArea,
            final double dpi) {

        final Scale scale = getScale(paintArea, dpi);
        final Scale correctedScale;
        final double scaleRatio;
        if (geodetic) {
            final double currentScaleDenominator = scale.getGeodeticDenominator(
                    getProjection(), dpi, getCenter());
            scaleRatio = scale.getDenominator(dpi) / currentScaleDenominator;
            correctedScale = scale.toResolution(scale.getResolution() / scaleRatio);
        } else {
            scaleRatio = 1;
            correctedScale = scale;
        }

        DistanceUnit unit = DistanceUnit.fromProjection(getProjection());
        final ZoomLevelSnapStrategy.SearchResult result = zoomLevelSnapStrategy.search(
                correctedScale, tolerance, zoomLevels);
        final Scale newScale;

        if (geodetic) {
            newScale = new Scale(
                    result.getScale(unit).getDenominator(PDF_DPI) * scaleRatio,
                    getProjection(), dpi);
        } else {
            newScale = result.getScale(unit);
        }

        return newScale;
    }
    /**
     * Calculate and return the scale of the map bounds.
     *
     * @param paintArea the paint area of the map.
     * @param dpi the dpi of the map
     */
    public abstract Scale getScale(final Rectangle paintArea, final double dpi);

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
    public abstract MapBounds zoomToScale(final Scale scale);

    /**
     * Get the center.
     *
     * @return the center position
     */
    public abstract Coordinate getCenter();

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

        return projection.equals(mapBounds.projection);
    }

    @Override
    public int hashCode() {
        return projection.hashCode();
    }
    // CHECKSTYLE:ON
}
