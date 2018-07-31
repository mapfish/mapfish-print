package org.mapfish.print.attribute.map;

import org.mapfish.print.parser.HasDefaultValue;

/**
 * Zoom the map to the features of a specific layer or all features of the map.
 */
public class ZoomToFeatures {
    private static final int DEFAULT_ZOOM_TO_MIN_MARGIN = 10;

    /**
     * The zoom type. Possible values:
     * <ul>
     * <li><code>extent</code> (default): Set the extent of the map so that all features
     * are visible.</li>
     * <li><code>center</code>: Set the center of the map to the center of the extent
     * of the features.</li>
     * </ul>
     */
    @HasDefaultValue
    public ZoomType zoomType = ZoomType.EXTENT;

    /**
     * The name of the layer whose features will be used. If not set, the features of all vector layers will
     * be used.
     */
    @HasDefaultValue
    public String layer;

    /**
     * The minimum scale that the map is zoomed to.
     */
    @HasDefaultValue
    public Double minScale;

    /**
     * The minimum margin (in px) from the features to the map border (default: 10).
     */
    @HasDefaultValue
    public Integer minMargin = DEFAULT_ZOOM_TO_MIN_MARGIN;

    /**
     * Make a copy.
     */
    public final ZoomToFeatures copy() {
        ZoomToFeatures obj = new ZoomToFeatures();
        obj.zoomType = this.zoomType;
        obj.minScale = this.minScale;
        obj.minMargin = this.minMargin;
        return obj;
    }

    /**
     * The zoom type.
     */
    public enum ZoomType {

        /**
         * Set the center of the map to the center of the extent of the features.
         */
        CENTER,

        /**
         * Set the extent of the map so that all features are visible.
         */
        EXTENT
    }
}
