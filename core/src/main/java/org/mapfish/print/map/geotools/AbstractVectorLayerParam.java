package org.mapfish.print.map.geotools;

import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.parser.HasDefaultValue;

/**
 * Common parameters for geotools vector layers.
 */
public abstract class AbstractVectorLayerParam extends AbstractLayerParams {
    /**
     * The style name of a style to apply to the features during rendering.  The style name must map to a
     * style in the template or the configuration objects.
     *
     * If no style is defined then the default style for the geometry type will be used.
     */
    @HasDefaultValue
    public String style;
    /**
     * Indicates if the layer is rendered as SVG.
     *
     * (will default to {@link org.mapfish.print.config.Configuration#defaultToSvg}).
     */
    @HasDefaultValue
    public Boolean renderAsSvg;
}
