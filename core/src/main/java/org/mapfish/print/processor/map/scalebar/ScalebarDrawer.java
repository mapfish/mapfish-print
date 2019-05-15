package org.mapfish.print.processor.map.scalebar;

import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Base class for drawing a scale bar.
 */
public abstract class ScalebarDrawer {
    /**
     * The graphics context.
     */
    private final Graphics2D graphics2d;
    private final AffineTransform transform;
    /**
     * Parameters for rendering the scalebar.
     */
    private final ScaleBarRenderSettings settings;
    /**
     * Parameters for the scalebar.
     */
    private final ScalebarAttributeValues params;

    /**
     * Constructor.
     *
     * @param graphics2d The graphics context.
     * @param settings Parameters for rendering the scalebar.
     */
    public ScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        this.graphics2d = graphics2d;
        this.transform = new AffineTransform(graphics2d.getTransform());
        this.settings = settings;
        this.params = settings.getParams();
    }

    /**
     * Start the rendering of the scalebar.
     */
    public final void draw() {
        AffineTransform transform = new AffineTransform(this.transform);
        transform.concatenate(getAlignmentTransform());

        // draw the background box
        this.graphics2d.setTransform(transform);
        if (this.params.getBackgroundColor().getAlpha() > 0) {
            this.graphics2d.setColor(this.params.getBackgroundColor());
            this.graphics2d.fillRect(0, 0, this.settings.getSize().width, this.settings.getSize().height);
        }

        //draw the labels
        this.graphics2d.setColor(this.params.getFontColor());
        drawLabels(transform, this.params.getOrientation(), this.params.getLabelRotation());

        //sets the transformation for drawing the bar and do it
        final AffineTransform lineTransform = new AffineTransform(transform);
        setLineTranslate(lineTransform);

        if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_LEFT ||
                this.params.getOrientation() == Orientation.VERTICAL_LABELS_RIGHT) {
            final AffineTransform rotate = AffineTransform.getQuadrantRotateInstance(1);
            lineTransform.concatenate(rotate);
        }

        this.graphics2d.setTransform(lineTransform);
        this.graphics2d.setStroke(new BasicStroke(this.settings.getLineWidth()));
        this.graphics2d.setColor(this.params.getColor());
        drawBar();
    }

    /**
     * Create a transformation which takes the alignment settings into account.
     */
    private AffineTransform getAlignmentTransform() {
        final int offsetX;
        switch (this.settings.getParams().getAlign()) {
            case LEFT:
                offsetX = 0;
                break;
            case RIGHT:
                offsetX = this.settings.getMaxSize().width - this.settings.getSize().width;
                break;
            case CENTER:
            default:
                offsetX = (int) Math
                        .floor(this.settings.getMaxSize().width / 2.0 - this.settings.getSize().width / 2.0);
                break;
        }

        final int offsetY;
        switch (this.settings.getParams().getVerticalAlign()) {
            case TOP:
                offsetY = 0;
                break;
            case BOTTOM:
                offsetY = this.settings.getMaxSize().height - this.settings.getSize().height;
                break;
            case MIDDLE:
            default:
                offsetY = (int) Math.floor(this.settings.getMaxSize().height / 2.0 -
                                                   this.settings.getSize().height / 2.0);
                break;
        }

        return AffineTransform.getTranslateInstance(Math.round(offsetX), Math.round(offsetY));
    }

    private void setLineTranslate(final AffineTransform lineTransform) {
        final float x;
        final float y;
        if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_BELOW) {
            x = this.settings.getPadding() + this.settings.getLeftLabelMargin();
            y = this.settings.getPadding() + this.settings.getBarSize();
        } else if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_ABOVE) {
            x = this.settings.getPadding() + this.settings.getLeftLabelMargin();
            y = this.settings.getPadding() + this.settings.getBarSize() + this.settings.getLabelDistance() +
                    Label.getRotatedHeight(this.settings.getMaxLabelSize(), this.params.getLabelRotation());
        } else if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_LEFT) {
            x = this.settings.getPadding() +
                    Label.getRotatedWidth(this.settings.getMaxLabelSize(), this.params.getLabelRotation()) +
                    this.settings.getLabelDistance();
            y = this.settings.getPadding() + this.settings.getTopLabelMargin();
        } else {  // if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_RIGHT)
            x = this.settings.getPadding();
            y = this.settings.getPadding() + this.settings.getTopLabelMargin();
        }
        lineTransform.translate(Math.round(x), Math.round(y));
    }

    /**
     * Sets 0,0 in the middle of the first tick mark at labelDistance away from it.
     */
    private void setLabelTranslate(final AffineTransform labelTransform) {
        final float x;
        final float y;
        if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_BELOW) {
            x = this.settings.getPadding() + this.settings.getLeftLabelMargin();
            y = this.settings.getPadding() + this.settings.getBarSize() + this.settings.getLabelDistance();
        } else if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_ABOVE) {
            x = this.settings.getPadding() + this.settings.getLeftLabelMargin();
            y = this.settings.getPadding() +
                    Label.getRotatedHeight(this.settings.getMaxLabelSize(), this.params.getLabelRotation());
        } else if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_LEFT) {
            x = this.settings.getPadding() +
                    Label.getRotatedWidth(this.settings.getMaxLabelSize(), this.params.getLabelRotation());
            y = this.settings.getPadding() + this.settings.getTopLabelMargin();
        } else { //if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_RIGHT)
            x = this.settings.getPadding() + this.settings.getBarSize() + this.settings.getLabelDistance();
            y = this.settings.getPadding() + this.settings.getTopLabelMargin();
        }
        labelTransform.translate(Math.round(x), Math.round(y));
    }

    /**
     * Draws the bar itself. The transformation is setup in a manner where the bar should be drawn into the
     * rectangle (0, 0) (intervals*intervalWidth, -barSize).
     */
    protected abstract void drawBar();

    private void drawLabels(
            final AffineTransform transform, final Orientation orientation,
            final float labelRotation) {
        float prevMargin = orientation.isHorizontal() ? this.settings.getMaxSize().width :
                this.settings.getMaxSize().height;

        final AffineTransform firstTickTransform = new AffineTransform(transform);
        setLabelTranslate(firstTickTransform);  // 0,0 is the center of the first label

        for (int i = this.settings.getLabels().size() - 1; i >= 0; i--) {
            final Label label = this.settings.getLabels().get(i);

            final float newMargin;
            final boolean shouldSkipLabel;
            final AffineTransform centerTransform = new AffineTransform(firstTickTransform);
            final float halfRotatedWidth = label.getRotatedWidth(labelRotation) / 2.0f;
            final float halfRotatedHeight = label.getRotatedHeight(labelRotation) / 2.0f;

            if (orientation.isHorizontal()) {
                centerTransform.concatenate(AffineTransform.getTranslateInstance(
                        Math.round(label.getGraphicOffset()),
                        Math.round(orientation == Orientation.HORIZONTAL_LABELS_BELOW ?
                                           halfRotatedHeight : -halfRotatedHeight)));
                shouldSkipLabel = label.getGraphicOffset() + halfRotatedWidth > prevMargin - 1;
                newMargin = label.getGraphicOffset() - halfRotatedWidth;
            } else {
                centerTransform.concatenate(AffineTransform.getTranslateInstance(
                        Math.round(orientation == Orientation.VERTICAL_LABELS_RIGHT ?
                                           halfRotatedWidth : -halfRotatedWidth),
                        Math.round(label.getGraphicOffset())));
                shouldSkipLabel = label.getGraphicOffset() + halfRotatedHeight > prevMargin - 1;
                newMargin = label.getGraphicOffset() - halfRotatedHeight;
            }
            if (labelRotation != 0.0) {
                centerTransform.concatenate(AffineTransform.getRotateInstance(labelRotation));
            }

            if (!shouldSkipLabel) {
                this.graphics2d.setTransform(centerTransform);
                // For some reason, we need to floor the coordinates for the text to be nicely centered
                label.getLabelLayout().draw(this.graphics2d, (float) Math.floor(-label.getWidth() / 2.0f),
                                            (float) Math.floor(label.getHeight() / 2.0f));
                prevMargin = newMargin;
            }
            // else: the label would be written over the previous one => ignore it
        }
    }

    public final Graphics2D getGraphics2d() {
        return this.graphics2d;
    }

    public final ScaleBarRenderSettings getSettings() {
        return this.settings;
    }

    public final ScalebarAttributeValues getParams() {
        return this.params;
    }
}
