package org.mapfish.print.map;

import org.mapfish.print.parser.HasDefaultValue;

/**
 * Contains common properties to all layers.
 *
 * @author Jesse on 11/7/2014.
 */
public class AbstractLayerParams {
    /**
     * The opacity of the image.
     */
    @HasDefaultValue
    public double opacity = 1.0;
    /**
     * The name of the layer.
     */
    @HasDefaultValue
    public String name = "";

    /**
     * Fail if a tile return an error.
     */
    @HasDefaultValue
    public boolean failOnError = false;
}
