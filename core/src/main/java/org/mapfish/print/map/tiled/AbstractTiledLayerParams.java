package org.mapfish.print.map.tiled;

import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.parser.HasDefaultValue;

import java.net.URISyntaxException;

/**
 * Contains the standard parameters for tiled layers.
 */
public abstract class AbstractTiledLayerParams extends AbstractLayerParams {
    /**
     * The name of the style (in Configuration or Template) to use when drawing the layer to the map.  This is
     * separate from the style in that it indicates how to draw the map.  It allows one to apply any of the
     * SLD raster styling.
     */
    @HasDefaultValue
    public String rasterStyle = "raster";

    /**
     * Constructor.
     */
    public AbstractTiledLayerParams() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy
     */
    public AbstractTiledLayerParams(final AbstractTiledLayerParams other) {
        super(other);
        this.rasterStyle = other.rasterStyle;
    }

    /**
     * Get the base url for all tile requests.  For example it might be 'http://server
     * .com/geoserver/gwc/service/wmts'.
     */
    public abstract String getBaseUrl();

    /**
     * Validates the provided base url.
     *
     * @return True, if the url is valid.
     */
    public abstract boolean validateBaseUrl();

    /**
     * Create a URL that is common to all image requests for this layer.  It will take the base url and append
     * all mergeable and custom params to the base url.
     */
    public abstract String createCommonUrl()
            throws URISyntaxException;
}
