package org.mapfish.print.processor.map.scalebar;


import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;
import org.mapfish.print.map.DistanceUnit;

import java.awt.Dimension;
import java.util.List;

/**
 * Parameters to render a scalebar.
 */
public class ScaleBarRenderSettings {

    private ScalebarAttributeValues params;
    private List<Label> labels;
    private DistanceUnit scaleUnit;
    private DistanceUnit intervalUnit;

    private float intervalLengthInPixels;
    private double intervalLengthInWorldUnits;

    private float leftLabelMargin;
    private float rightLabelMargin;

    private int barSize;
    private int labelDistance;
    private int numSubIntervals;

    private int lineWidth;
    private Dimension maxLabelSize;
    private Dimension size;
    private Dimension maxSize;
    private float topLabelMargin;
    private float bottomLabelMargin;
    private int padding;

    public final double getIntervalLengthInWorldUnits() {
        return this.intervalLengthInWorldUnits;
    }

    public final void setIntervalLengthInWorldUnits(final double intervalLengthInWorldUnits) {
        this.intervalLengthInWorldUnits = intervalLengthInWorldUnits;
    }

    public final List<Label> getLabels() {
        return this.labels;
    }

    public final void setLabels(final List<Label> labels) {
        this.labels = labels;
    }

    public final float getIntervalLengthInPixels() {
        return this.intervalLengthInPixels;
    }

    public final void setIntervalLengthInPixels(final float intervalLengthInPixels) {
        this.intervalLengthInPixels = intervalLengthInPixels;
    }

    public final float getLeftLabelMargin() {
        return this.leftLabelMargin;
    }

    public final void setLeftLabelMargin(final float leftLabelMargin) {
        this.leftLabelMargin = leftLabelMargin;
    }

    public final float getRightLabelMargin() {
        return this.rightLabelMargin;
    }

    public final void setRightLabelMargin(final float rightLabelMargin) {
        this.rightLabelMargin = rightLabelMargin;
    }

    public final float getTopLabelMargin() {
        return this.topLabelMargin;
    }

    public final void setTopLabelMargin(final float topLabelMargin) {
        this.topLabelMargin = topLabelMargin;
    }

    public final float getBottomLabelMargin() {
        return this.bottomLabelMargin;
    }

    public final void setBottomLabelMargin(final float bottomLabelMargin) {
        this.bottomLabelMargin = bottomLabelMargin;
    }

    public final DistanceUnit getScaleUnit() {
        return this.scaleUnit;
    }

    public final void setScaleUnit(final DistanceUnit scaleUnit) {
        this.scaleUnit = scaleUnit;
    }

    public final DistanceUnit getIntervalUnit() {
        return this.intervalUnit;
    }

    public final void setIntervalUnit(final DistanceUnit intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public final ScalebarAttributeValues getParams() {
        return this.params;
    }

    public final void setParams(final ScalebarAttributeValues params) {
        this.params = params;
    }

    public final int getLabelDistance() {
        return this.labelDistance;
    }

    public final void setLabelDistance(final int labelDistance) {
        this.labelDistance = labelDistance;
    }

    public final int getLineWidth() {
        return this.lineWidth;
    }

    public final void setLineWidth(final int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public final int getBarSize() {
        return this.barSize;
    }

    public final void setBarSize(final int barSize) {
        this.barSize = barSize;
    }

    public final int getNumSubIntervals() {
        return this.numSubIntervals;
    }

    public final void setNumSubIntervals(final int numSubIntervals) {
        this.numSubIntervals = numSubIntervals;
    }

    public final Dimension getMaxLabelSize() {
        return this.maxLabelSize;
    }

    public final void setMaxLabelSize(final Dimension maxLabelSize) {
        this.maxLabelSize = maxLabelSize;
    }

    public final Dimension getSize() {
        return this.size;
    }

    public final void setSize(final Dimension size) {
        this.size = size;
    }

    public final Dimension getMaxSize() {
        return this.maxSize;
    }

    public final void setMaxSize(final Dimension maxSize) {
        this.maxSize = maxSize;
    }

    public final int getPadding() {
        return this.padding;
    }

    public final void setPadding(final int padding) {
        this.padding = padding;
    }
}
