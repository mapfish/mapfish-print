package org.mapfish.print.processor.map.scalebar;

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
     * @param labelLayout   Layout for the label.
     * @param orientation   Scalebar orientation.
     */
    public Label(final float graphicOffset, final TextLayout labelLayout, final Orientation orientation) {
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
}

