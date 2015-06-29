package org.mapfish.print.map.geotools.grid;

import org.geotools.styling.Style;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;

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
         * @param template
         * @param layerData
         */
        Style defaultStyle(Template template, GridParam layerData);

        /**
         * Return the features for the grid.
         * @param template
         * @param layerData
         */
        FeatureSourceSupplier createFeatureSource(Template template, GridParam layerData);
    }
}
