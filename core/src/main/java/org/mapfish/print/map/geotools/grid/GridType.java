package org.mapfish.print.map.geotools.grid;

import org.geotools.styling.Style;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;

/**
 * The supported Grid types.
 */
public enum GridType {
    /**
     * Represents a Grid that consists of lines.
     */
    LINES(new LineGridStrategy()),
    /**
     * Represents a Grid that consists of points where the lines would intersect if the grid was a set of
     * lines.
     */
    POINTS(new PointGridStrategy());

    /**
     * The strategy to use for this type.
     */
    final GridTypeStrategy strategy;

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
         * @param template The template the grid layer is part of
         * @param layerData the layer parameters
         */
        Style defaultStyle(Template template, GridParam layerData);

        /**
         * Return the features for the grid. During the creation of the features the grid labels should be
         * added to the label collector for rendering at the end of the process.
         *
         * @param template The template the grid layer is part of
         * @param layerData the layer parameters
         * @param labels the collector for the labels.
         */
        FeatureSourceSupplier createFeatureSource(
                Template template, GridParam layerData, LabelPositionCollector labels);
    }

}
