/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config.layout;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import org.mapfish.print.*;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.scalebar.Direction;
import org.mapfish.print.scalebar.Label;
import org.mapfish.print.scalebar.ScalebarDrawer;
import org.mapfish.print.scalebar.Type;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Block for drawing a !scalebar block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Scalebarblock
 */
public class ScalebarBlock extends FontBlock {
    private int maxSize = 150;

    private Type type = Type.LINE;

    private int intervals = 3;

    private boolean subIntervals = false;

    private DistanceUnit units = null;

    private Integer barSize = null;

    private Direction barDirection = Direction.UP;

    private Direction textDirection = Direction.UP;

    private Integer labelDistance = null;

    private String color = "black";

    /**
     * The background color of the odd intervals (only for the scalebar of type "bar")
     */
    private String barBgColor = null;

    private Double lineWidth = null;


    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        final PJsonObject globalParams = context.getGlobalParams();
        final DistanceUnit mapUnits = DistanceUnit.fromString(globalParams.getString("units"));
        if (mapUnits == null) {
            throw new InvalidJsonValueException(globalParams, "units", globalParams.getString("units"));
        }
        DistanceUnit scaleUnit = (units != null ? units : mapUnits);
        final int scale = context.getLayout().getMainPage().getMap().createTransformer(context, params).getScale();

        final double maxWidthIntervaleDistance = DistanceUnit.PT.convertTo(maxSize, scaleUnit) * scale / intervals;
        final double intervalDistance = getNearestNiceValue(maxWidthIntervaleDistance, scaleUnit);

        final Font pdfFont = getPdfFont();
        tryLayout(context, target, pdfFont, scaleUnit, scale, intervalDistance, 0);
    }

    /**
     * Try recursively to find the correct layout.
     */
    private void tryLayout(RenderingContext context, PdfElement target, Font pdfFont, DistanceUnit scaleUnit, int scale, double intervalDistance, int tryNumber) throws DocumentException {
        if (tryNumber > 3) {
            //noinspection ThrowableInstanceNeverThrown
            context.addError(new InvalidValueException("maxSize too small", maxSize));
            return;
        }

        DistanceUnit intervalUnit = DistanceUnit.getBestUnit(intervalDistance, scaleUnit);
        final float intervalPaperWidth = (float) scaleUnit.convertTo(intervalDistance / scale, DistanceUnit.PT);

        //compute the label positions
        final List<Label> labels = new ArrayList<Label>(intervals + 1);
        final float leftLabelMargin;
        final float rightLabelMargin;
        final BaseFont baseFont = pdfFont.getCalculatedBaseFont(false);
        if (intervals > 1 || subIntervals) {
            //the label will be centered under each tick marks
            for (int i = 0; i <= intervals; ++i) {
                String labelText = createLabelText(scaleUnit, intervalDistance * i, intervalUnit);
                if (i == intervals) {
                    labelText += intervalUnit;
                }
                labels.add(new Label(intervalPaperWidth * i, labelText, baseFont, getFontSize(),
                        !barDirection.isSameOrientation(textDirection)));
            }
            leftLabelMargin = labels.get(0).width / 2.0f;
            rightLabelMargin = labels.get(labels.size() - 1).width / 2.0f;
        } else {
            //if there is only one interval, place the label centered between the two tick marks
            final Label label = new Label(intervalPaperWidth / 2.0f,
                    createLabelText(scaleUnit, intervalDistance, intervalUnit) + intervalUnit, baseFont, getFontSize(), !barDirection.isSameOrientation(textDirection));
            labels.add(label);
            leftLabelMargin = rightLabelMargin = Math.max(0.0f, label.width - intervalPaperWidth) / 2.0f;
        }


        if (intervals * intervalPaperWidth + leftLabelMargin + rightLabelMargin <= maxSize) {
            //the layout fits the maxSize
            doLayout(context, target, pdfFont, labels, intervalPaperWidth, scaleUnit, intervalDistance, intervalUnit,
                    leftLabelMargin, rightLabelMargin);
        } else {
            //not enough room because of the labels, try a smaller bar
            double nextIntervalDistance = getNearestNiceValue(intervalDistance * 0.9, scaleUnit);
            tryLayout(context, target, pdfFont, scaleUnit, scale, nextIntervalDistance, tryNumber + 1);
        }
    }

    /**
     * Called when the position of the labels and their content is known.
     * <p/>
     * Creates the drawer and schedule it for drawing when the position of the block is known.
     */
    private void doLayout(RenderingContext context, PdfElement target, Font pdfFont, List<Label> labels,
                          float intervalWidth, DistanceUnit scaleUnit, double intervalDistance,
                          DistanceUnit intervalUnit, float leftLabelPaperMargin, float rightLabelPaperMargin) throws DocumentException {
        float maxLabelHeight = 0.0f;
        float maxLabelWidth = 0.0f;
        for (int i = 0; i < labels.size(); i++) {
            Label label = labels.get(i);
            maxLabelHeight = Math.max(maxLabelHeight, label.height);
            maxLabelWidth = Math.max(maxLabelWidth, label.width);
        }
        final float straightWidth = intervalWidth * intervals + leftLabelPaperMargin + rightLabelPaperMargin;
        final float straightHeight = getBarSize() + getLabelDistance() + maxLabelHeight;
        final float width;
        final float height;
        if (barDirection == Direction.DOWN || barDirection == Direction.UP) {
            width = straightWidth;
            height = straightHeight;
        } else {
            //noinspection SuspiciousNameCombination
            width = straightHeight;
            //noinspection SuspiciousNameCombination
            height = straightWidth;
        }

        int numSubIntervals = 1;
        if (subIntervals) {
            numSubIntervals = getNbSubIntervals(scaleUnit, intervalDistance, intervalUnit);
        }

        ChunkDrawer drawer = ScalebarDrawer.create(context.getCustomBlocks(), this, type, labels, getBarSize(), getLabelDistance(),
                numSubIntervals, intervalWidth, pdfFont, leftLabelPaperMargin, rightLabelPaperMargin, maxLabelWidth, maxLabelHeight);
        target.add(PDFUtils.createPlaceholderTable(width, height, spacingAfter, drawer, align, context.getCustomBlocks()));
    }

    /**
     * Format the label text.
     */
    private String createLabelText(DistanceUnit scaleUnit, double value, DistanceUnit intervalUnit) {
        final double scaledValue = scaleUnit.convertTo(value, intervalUnit);
        return Long.toString(Math.round(scaledValue));
    }

    /**
     * Reduce the given value to the nearest 1 significant digit number starting
     * with 1, 2 or 5.
     */
    private double getNearestNiceValue(double value, DistanceUnit scaleUnit) {
        DistanceUnit bestUnit = DistanceUnit.getBestUnit(value, scaleUnit);
        double factor = scaleUnit.convertTo(1.0, bestUnit);

        // nearest power of 10 lower than value
        int digits = (int) (Math.log(value * factor) / Math.log(10));
        double pow10 = Math.pow(10, digits);

        // ok, find first character
        double firstChar = value * factor / pow10;

        // right, put it into the correct bracket
        int barLen;
        if (firstChar >= 10.0) {
            barLen = 10;
        } else if (firstChar >= 5.0) {
            barLen = 5;
        } else if (firstChar >= 2.0) {
            barLen = 2;
        } else {
            barLen = 1;
        }

        // scale it up the correct power of 10
        return barLen * pow10 / factor;
    }

    /**
     * @return The "nicest" number of sub intervals in function of the interval distance.
     */
    private int getNbSubIntervals(DistanceUnit scaleUnit, double intervalDistance, DistanceUnit intervalUnit) {
        double value = scaleUnit.convertTo(intervalDistance, intervalUnit);
        int digits = (int) (Math.log(value) / Math.log(10));
        double pow10 = Math.pow(10, digits);

        // ok, find first character
        int firstChar = (int) (value / pow10);
        switch (firstChar) {
            case 1:
                return 2;
            case 2:
                return 2;
            case 5:
                return 5;
            case 10:
                return 2;
            default:
                throw new RuntimeException("Invalid interval: " + value + intervalUnit + " (" + firstChar + ")");
        }
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        if (maxSize <= 0) throw new InvalidValueException("maxSize", maxSize);
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setIntervals(int intervals) {
        if (intervals < 1) {
            throw new InvalidValueException("intervals", intervals);
        }
        this.intervals = intervals;
    }

    public void setSubIntervals(boolean subIntervals) {
        this.subIntervals = subIntervals;
    }

    public void setUnits(DistanceUnit units) {
        this.units = units;
    }

    public void setBarSize(int barSize) {
        this.barSize = barSize;
        if (barSize < 0) throw new InvalidValueException("barSize", barSize);
    }

    public void setBarDirection(Direction barDirection) {
        this.barDirection = barDirection;
    }

    public void setTextDirection(Direction textDirection) {
        this.textDirection = textDirection;
    }

    public void setLabelDistance(int labelDistance) {
        this.labelDistance = labelDistance;
    }

    public void setBarBgColor(String barBgColor) {
        this.barBgColor = barBgColor;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        if (lineWidth < 0) throw new InvalidValueException("lineWidth", lineWidth);
    }

    public int getBarSize() {
        if (barSize != null) {
            return barSize;
        } else {
            return maxSize / 30;
        }
    }

    public int getLabelDistance() {
        if (labelDistance != null) {
            return labelDistance;
        } else {
            return maxSize / 40;
        }
    }

    public double getFontSize() {
        if (fontSize != null) {
            return fontSize;
        } else {
            return maxSize * 10.0 / 200.0;
        }
    }

    public double getLineWidth() {
        if (lineWidth != null) {
            return lineWidth;
        } else {
            return maxSize / 150.0;
        }
    }

    public Direction getBarDirection() {
        return barDirection;
    }

    public Direction getTextDirection() {
        return textDirection;
    }

    public int getIntervals() {
        return intervals;
    }

    public Color getBarBgColorVal() {
        return ColorWrapper.convertColor(barBgColor);
    }

    public Color getColorVal() {
        return ColorWrapper.convertColor(color);
    }
}
