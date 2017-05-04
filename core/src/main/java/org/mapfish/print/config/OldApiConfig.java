package org.mapfish.print.config;

import java.util.List;

/**
 * <p>Configuration options for how requests to the old API are handled.</p>
 *
 * <p>Example</p>
 * <pre><code>
 * oldApi: !oldApi
 *   layersFirstIsBaseLayer: false
 *   wmsReverseLayers: true
 * templates:
 *   ..</code></pre>
 */
public final class OldApiConfig implements ConfigurationObject {
    private boolean layersFirstIsBaseLayer = true;
    private boolean wmsReverseLayers = false;

    /**
     * If true then the first layer in the layers array in the JSON request is the bottom layer of the map.
     */
    public boolean isLayersFirstIsBaseLayer() {
        return this.layersFirstIsBaseLayer;
    }

    /**
     * If true then the first layer in the layers array in the JSON request is the bottom layer of the map.
     * @param layersFirstIsBaseLayer If true then the first layer in the layers array in the JSON request is the bottom layer of the map.
     */
    public void setLayersFirstIsBaseLayer(final boolean layersFirstIsBaseLayer) {
        this.layersFirstIsBaseLayer = layersFirstIsBaseLayer;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // nothing to do
    }

    /**
     * If true then the layer order coming from the old API client will be reversed for the layers within a WMS request.
     */
    public boolean isWmsReverseLayers() {
        return this.wmsReverseLayers;
    }

    /**
     * Set if the layer order coming from the old API client will be reversed for the layers within a WMS request.
     *
     * @param wmsReverseLayers if true then the layer order will be reversed
     */
    public void setWmsReverseLayers(final boolean wmsReverseLayers) {
        this.wmsReverseLayers = wmsReverseLayers;
    }
}
