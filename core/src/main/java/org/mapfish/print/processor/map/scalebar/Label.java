package org.mapfish.print.processor.map.scalebar;

import java.awt.Dimension;
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

    private TextLayout labelLayout;

    /**
     * Constructor.
     * @param graphicOffset Position offset.
     * @param labelLayout Layout for the label.
     */
    public Label(final float graphicOffset, final TextLayout labelLayout) {
        this.graphicOffset = graphicOffset;
        this.labelLayout = labelLayout;
        this.width = (float) labelLayout.getBounds().getWidth();
        this.height = (float) labelLayout.getBounds().getHeight();
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
     * @param angleDegree Angle in degree
     * @return The width of the rotated label
     */
    public float getRotatedWidth(final double angleDegree) {
        return getRotatedWidth(this.width, this.height, angleDegree);
    }

    /**
     * @param angleDegree Angle in degree
     * @return The height of the rotated label
     */
    public float getRotatedHeight(final double angleDegree) {
        return getRotatedHeight(this.width, this.height, angleDegree);
    }

    /**
     * @param width Unrotated width
     * @param height Unrotated height
     * @param angleDegree Angle in degree
     * @return The width of the rotated label
     */
    private static float getRotatedWidth(final float width, final float height, final double angleDegree) {
        final double angle = Math.toRadians(angleDegree);
        return (float) (Math.abs(width * Math.cos(angle)) + Math.abs(height * Math.sin(angle)));
    }

    /**
     * @param width Unrotated width
     * @param height Unrotated height
     * @param angleDegree Angle in degree
     * @return The height of the rotated label
     */
    private static float getRotatedHeight(final float width, final float height, final double angleDegree) {
        final double angle = Math.toRadians(angleDegree);
        return (float) (Math.abs(height * Math.cos(angle)) + Math.abs(width * Math.sin(angle)));
    }

    /**
     * @param dimension Unrotated dimension
     * @param angleDegree Angle in degree
     * @return The height of the rotated label
     */
    public static float getRotatedHeight(final Dimension dimension, final int angleDegree) {
        return getRotatedHeight(dimension.width, dimension.height, angleDegree);
    }

    /**
     * @param dimension Unrotated dimension
     * @param angleDegree Angle in degree
     * @return The width of the rotated label
     */
    public static float getRotatedWidth(final Dimension dimension, final int angleDegree) {
        return getRotatedWidth(dimension.width, dimension.height, angleDegree);
    }
}
