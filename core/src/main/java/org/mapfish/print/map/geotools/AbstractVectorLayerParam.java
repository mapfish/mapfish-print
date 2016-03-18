package org.mapfish.print.map.geotools;

import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.parser.HasDefaultValue;

/**
 * Common parameters for geotools vector layers.
 *
 * @author Jesse on 7/2/2014.
 */
public abstract class AbstractVectorLayerParam extends AbstractLayerParams {
    /**
     * The style name of a style to apply to the features during rendering.  The style name must map to a style in the
     * template or the configuration objects.
     * <p></p>
     * If no style is defined then the default style for the geometry type will be used.
     */
    @HasDefaultValue
    public String style;
    /**
     * Indicates if the layer is rendered as SVG.
     * <p></p>
     * (will default to {@link org.mapfish.print.config.Configuration#defaultStyle}).
     */
    @HasDefaultValue
    public Boolean renderAsSvg;
}
