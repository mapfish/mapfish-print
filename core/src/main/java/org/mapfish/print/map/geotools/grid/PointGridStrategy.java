package org.mapfish.print.map.geotools.grid;

import org.geotools.styling.Style;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;

/**
 * Strategy for creating the style and features for the grid when the grid consists of lines.
 *
 * @author Jesse on 6/29/2015.
 */
class PointGridStrategy implements GridType.GridTypeStrategy {
    @Override
    public Style defaultStyle(final Template template, final GridParam layerData) {
        return null;
    }

    @Override
    public FeatureSourceSupplier createFeatureSource(final Template template, final GridParam layerData) {
        return null;
    }
}
