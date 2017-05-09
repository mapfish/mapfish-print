package org.mapfish.print.map.image.wms;

import com.vividsolutions.jts.util.Assert;

import org.mapfish.print.map.tiled.AbstractWMXLayerParams;
import org.mapfish.print.parser.HasDefaultValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Arrays;


/**
 * Layer parameters for WMS layer.
 */
public class WmsLayerParam extends AbstractWMXLayerParams {
    private static final Logger LOGGER = LoggerFactory.getLogger(WmsLayerParam.class);

    /**
     * The base URL for the WMS.  Used for making WMS requests.
     */
    public String baseURL;
    /**
     * The wms layer to request in the GetMap request.  The order is important.  It is the order that they will appear in the
     * request.
     *
     * As with the WMS specification, the first layer will be the first layer drawn on the map (the bottom/base layer) of the map.  This
     * means that layer at position 0 in the array will covered by layer 1 (where not transparent) and so on.
     */
    public String[] layers;

    /**
     * The styles to apply to the layers.  If this is defined there should be the same number as the layers and the style are applied
     * to the layer in the {@link #layers} field.
     */
    @HasDefaultValue
    public String[] styles;

    /**
     * The WMS version to use when making requests.
     */
    @HasDefaultValue
    public String version = "1.1.1";

    /**
     *  If true transform the map angle to customParams.angle for GeoServer, and customParams.map_angle for MapServer.
     */
    @HasDefaultValue
    public boolean useNativeAngle = false;

    /**
     *  The server type ("mapserver", "geoserver" or "qgisserver"). By specifying the server type
     *  vendor specific parameters (like for the DPI value) can be used when making the request.
     */
    @HasDefaultValue
    public ServerType serverType;

    /**
     * The format of the image. for example image/png, image/jpeg, etc...
     */
    @HasDefaultValue
    public String imageFormat = "image/png";


    @Override
    public final String getBaseUrl() {
        return this.baseURL;
    }

    /**
     * Validate some of the properties of this layer.
     */
    public void postConstruct() throws URISyntaxException {
        WmsVersion.lookup(this.version);
        Assert.isTrue(validateBaseUrl(), "invalid baseURL");

        Assert.isTrue(this.layers.length > 0, "There must be at least one layer defined for a WMS request to make sense");

        // OpenLayers 2 compatibility.  It will post a single empty style no matter how many layers there are

        if (this.styles != null && this.styles.length != this.layers.length && this.styles.length == 1 && this.styles[0].trim().isEmpty()) {
            this.styles = null;
        } else {
            Assert.isTrue(this.styles == null || this.layers.length == this.styles.length,
                    "If styles are defined then there must be one for each layer.  Number of layers: " + this.layers.length + "\nStyles: "
                    + Arrays.toString(this.styles));
        }
        if (this.imageFormat.indexOf('/') < 0) {
            LOGGER.warn("The format should be a mime type");
            this.imageFormat = "image/" + this.imageFormat;
        }
    }

    /**
     * The WMS server type.
     */
    public enum ServerType {
        /**
         * MapServer.
         */
        MAPSERVER,
        /**
         * GeoServer.
         */
        GEOSERVER,
        /**
         * QGIS Server.
         */
        QGISSERVER
    }
}
