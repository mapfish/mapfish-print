package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.annotation.Nonnull;

/**
 * The supported Grid types.
 *
 * @author Jesse on 6/29/2015.
 */
public enum GridType {
    /**
     * Represents a Grid that consists of lines.
     */
    LINES(new LineGridStrategy()),
    /**
     * Represents a Grid that consists of points where the lines would intersect if the grid was a set of lines.
     */
    POINTS(new PointGridStrategy());

    /**
     * The strategy to use for this type.
     */
    // CSOFF:VisibilityModifier
    final GridTypeStrategy strategy;
    // CSON:VisibilityModifier

    GridType(final GridTypeStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * The code for getting the features and default style for the {@link GridType}.
     */
    interface GridTypeStrategy {
        /**
         * Returns the default style for this type.
         *
         * @param template
         * @param layerData
         */
        Style defaultStyle(Template template, GridParam layerData);

        /**
         * Return the features for the grid.
         *
         * @param template
         * @param layerData
         */
        FeatureSourceSupplier createFeatureSource(Template template, GridParam layerData);
    }

    /**
     * Create the grid feature type.
     *
     * @param mapContext the map context containing the information about the map the grid will be added to.
     * @param geomClass the geometry type
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
     * @param value the value of the line
     * @param unit the unit that the value is in
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
