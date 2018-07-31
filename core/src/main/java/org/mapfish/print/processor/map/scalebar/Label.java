package org.mapfish.print.processor.map.scalebar;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;


/**
 * Position, size and content of a label.
 */
public class Label {
    /**
     * Position of the label, relative to the first tick of the bar.
     */
    private final float graphicOffset;

    /**
     * Width of the label.
     */
    private final float width;

    /**
     * Height of the label.
     */
    private final float height;

    private final TextLayout labelLayout;

    /**
     * Constructor.
     *
     * @param graphicOffset Position offset.
     * @param labelLayout Layout for the label.
     * @param graphics2D Where it is going to be rendered (more accurate size computation
     */
    public Label(final float graphicOffset, final TextLayout labelLayout, final Graphics2D graphics2D) {
        this.graphicOffset = graphicOffset;
        this.labelLayout = labelLayout;
        Rectangle bounds = this.labelLayout.getPixelBounds(graphics2D.getFontRenderContext(), 0, 0);
        // Not sure on the impact when tha label are rotated
        assert graphics2D.getTransform().getScaleX() == graphics2D.getTransform().getScaleY();
        this.width = (float) (bounds.getWidth() / graphics2D.getTransform().getScaleX());
        this.height = (float) (bounds.getHeight() / graphics2D.getTransform().getScaleY());
    }

    /**
     * @param width Unrotated width
     * @param height Unrotated height
     * @param angle Angle
     * @return The width of the rotated label
     */
    private static float getRotatedWidth(final float width, final float height, final float angle) {
        return (float) (Math.abs(width * Math.cos(angle)) + Math.abs(height * Math.sin(angle)));
    }

    /**
     * @param width Unrotated width
     * @param height Unrotated height
     * @param angle Angle
     * @return The height of the rotated label
     */
    private static float getRotatedHeight(final float width, final float height, final float angle) {
        return (float) (Math.abs(height * Math.cos(angle)) + Math.abs(width * Math.sin(angle)));
    }

    /**
     * @param dimension Unrotated dimension
     * @param angle Angle
     * @return The height of the rotated label
     */
    public static float getRotatedHeight(final Dimension dimension, final float angle) {
        return getRotatedHeight(dimension.width, dimension.height, angle);
    }

    /**
     * @param dimension Unrotated dimension
     * @param angle Angle
     * @return The width of the rotated label
     */
    public static float getRotatedWidth(final Dimension dimension, final float angle) {
        return getRotatedWidth(dimension.width, dimension.height, angle);
    }

    public final float getWidth() {
        return this.width;
    }

    public final float getHeight() {
        return this.height;
    }

    public final float getGraphicOffset() {
        return this.graphicOffset;
    }

    public final TextLayout getLabelLayout() {
        return this.labelLayout;
    }

    /**
     * @param angle Angle
     * @return The width of the rotated label
     */
    public float getRotatedWidth(final float angle) {
        return getRotatedWidth(this.width, this.height, angle);
    }

    /**
     * @param angle Angle
     * @return The height of the rotated label
     */
    public float getRotatedHeight(final float angle) {
        return getRotatedHeight(this.width, this.height, angle);
    }
}
