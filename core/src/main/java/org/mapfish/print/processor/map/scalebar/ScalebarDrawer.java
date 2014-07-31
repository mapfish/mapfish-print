/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

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
     * @param graphics2d    The graphics context.
     * @param settings      Parameters for rendering the scalebar.
     */
    public ScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        this.graphics2d = graphics2d;
        this.settings = settings;
        this.params = settings.getParams();
    }

    /**
     * Create a {@link ScalebarDrawer} instance for the given type.
     *
     * @param type          The scalebar type.
     * @param graphics2d    The graphics context.
     * @param settings      Parameters for rendering the scalebar.
     */
    public static ScalebarDrawer create(final Type type, final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        switch (type) {
            case BAR:
                return new BarScalebarDrawer(graphics2d, settings);
            case BAR_SUB:
                return new BarSubScalebarDrawer(graphics2d, settings);
            case LINE:
                return new LineScalebarDrawer(graphics2d, settings);
            default:
                throw new RuntimeException("Unknown scalebar type: " + type);
        }
    }

    /**
     * Start the rendering of the scalebar.
     */
    public final void draw() {
        // draw the background box
        this.graphics2d.setColor(this.params.getBackgroundColor());
        this.graphics2d.fillRect(0, 0, this.settings.getSize().width, this.settings.getSize().height);

        //sets the transformation for drawing the labels and do it
        final AffineTransform labelTransform = AffineTransform.getTranslateInstance(0, 0);
        setLabelTranslate(labelTransform);

        this.graphics2d.setTransform(labelTransform);
        this.graphics2d.setColor(this.params.getFontColor());
        drawLabels(this.params.getOrientation());

        //sets the transformation for drawing the bar and do it
        final AffineTransform lineTransform = AffineTransform.getTranslateInstance(0, 0);
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

    private void setLineTranslate(final AffineTransform lineTransform) {
        if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_BELOW) {
            lineTransform.translate(
                    this.settings.getPadding() + this.settings.getLeftLabelMargin(),
                    this.settings.getPadding() + this.settings.getBarSize());
        } else if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_ABOVE) {
            lineTransform.translate(
                    this.settings.getPadding() + this.settings.getLeftLabelMargin(),
                    this.settings.getPadding() + this.settings.getBarSize() + this.settings.getLabelDistance()
                    + this.settings.getMaxLabelSize().height);
        } else if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_LEFT) {
            lineTransform.translate(
                    this.settings.getPadding() + this.settings.getMaxLabelSize().width + this.settings.getLabelDistance(),
                    this.settings.getPadding() + this.settings.getTopLabelMargin());
        } else if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_RIGHT) {
            lineTransform.translate(
                    this.settings.getPadding(),
                    this.settings.getPadding() + this.settings.getTopLabelMargin());
        }
    }

    private void setLabelTranslate(final AffineTransform labelTransform) {
        if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_BELOW) {
            labelTransform.translate(
                    this.settings.getPadding() + this.settings.getLeftLabelMargin(),
                    this.settings.getPadding() + this.settings.getBarSize() + this.settings.getLabelDistance()
                    + this.settings.getMaxLabelSize().height);
        } else if (this.params.getOrientation() == Orientation.HORIZONTAL_LABELS_ABOVE) {
            labelTransform.translate(
                    this.settings.getPadding() + this.settings.getLeftLabelMargin(),
                    this.settings.getPadding() + this.settings.getMaxLabelSize().height);
        } else if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_LEFT) {
            labelTransform.translate(
                    this.settings.getPadding(),
                    this.settings.getPadding() + this.settings.getTopLabelMargin());
        } else if (this.params.getOrientation() == Orientation.VERTICAL_LABELS_RIGHT) {
            labelTransform.translate(
                    this.settings.getPadding() + this.settings.getBarSize() + this.settings.getLabelDistance(),
                    this.settings.getPadding() + this.settings.getTopLabelMargin());
        }
    }

    /**
     * Draws the bar itself. The transformation is setup in a manner where the
     * bar should be drawn into the rectangle (0, 0) (intervals*intervalWidth, -barSize).
     */
    protected abstract void drawBar();

    private float getTotalLength(final Orientation orientation) {
        if (orientation.isHorizontal()) {
            return this.settings.getIntervalLengthInPixels() * this.params.intervals
                    + this.settings.getLeftLabelMargin() + this.settings.getRightLabelMargin();
        } else {
            return this.settings.getIntervalLengthInPixels() * this.params.intervals
                    + this.settings.getTopLabelMargin() + this.settings.getBottomLabelMargin();
        }
    }

    private void drawLabels(final Orientation orientation) {
        float prevPos = getTotalLength(orientation);

        for (int i = this.settings.getLabels().size() - 1; i >= 0; i--) {
            final Label label = this.settings.getLabels().get(i);
            final float posX;
            final float posY;
            final float newPos;
            final boolean shouldSkipLabel;
            if (orientation.isHorizontal()) {
                final float offsetH = -label.getWidth() / 2;
                posX = label.getGraphicOffset() + offsetH;
                posY = 0;
                shouldSkipLabel = label.getGraphicOffset() + Math.abs(offsetH) > prevPos - 1;
                newPos = label.getGraphicOffset() - Math.abs(offsetH);
            } else {
                final float offsetV = label.getHeight() / 2;
                if (orientation == Orientation.VERTICAL_LABELS_LEFT) {
                    posX = this.settings.getMaxLabelSize().width - label.getWidth();
                } else {
                    posX = 0;
                }
                posY = label.getGraphicOffset() + offsetV;
                shouldSkipLabel = label.getGraphicOffset() + offsetV > prevPos - 1;
                newPos = label.getGraphicOffset() - Math.abs(offsetV);
            }

            if (!shouldSkipLabel) {
                label.getLabelLayout().draw(this.graphics2d, posX, posY);
                prevPos = newPos;
            } else {
                //the label would be written over the previous one => ignore it
            }
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
