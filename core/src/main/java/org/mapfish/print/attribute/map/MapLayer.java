package org.mapfish.print.attribute.map;

import com.google.common.base.Optional;
import org.mapfish.print.http.MfClientHttpRequestFactory;

import java.awt.Graphics2D;

/**
 * Encapsulates the data required to load map data for a layer and render it.
 */
public interface MapLayer {

    /**
     * Attempt to add the layer this layer so that both can be rendered as a single layer.
     * <p></p>
     * For example:
     * 2 WMS layers from the same WMS server can be combined into a single WMS layer and the map can be rendered
     * with a single WMS request.
     *
     * @param newLayer the layer to combine with this layer.  The new layer will be rendered <em>below</em> the current layer.
     * @return If the two layers can be combined then a map layer representing the two layers will be returned.  If the two layers
     * cannot be combined then Option.absent() will be returned.
     */
    Optional<MapLayer> tryAddLayer(MapLayer newLayer);

    /**
     * Get the scale ratio between the tiles resolution and the target resolution.
     * Used to don't scale the tiles on tiled layer.
     */
    double getImageBufferScaling();

    /**
     * Render the layer to the graphics2D object.
     * @param transformer the map transformer containing the map bounds and size.
     */
    void prepareRender(final MapfishMapContext transformer);

    /**
     * Render the layer to the graphics2D object.
     * @param graphics2D the graphics object.
     * @param clientHttpRequestFactory The factory to use for making http requests.
     * @param transformer the map transformer containing the map bounds and size.
     */
    void render(
            final Graphics2D graphics2D,
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final MapfishMapContext transformer);

    /**
     * Indicate if the layer supports native rotation (e.g. WMS layers with 
     * the "angle" parameter).
     * @return True if the layer itself takes care of rotating.
     */
    boolean supportsNativeRotation();

    /**
     * The layer name.
     */
    String getName();
}
