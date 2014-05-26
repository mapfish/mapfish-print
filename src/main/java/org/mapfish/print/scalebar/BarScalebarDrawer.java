/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.scalebar;

import com.itextpdf.text.BaseColor;
import java.util.List;

import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.config.layout.ScalebarBlock;

import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfContentByte;

/**
 * Draw a bar with alternating black and white zones marking the sub-intervals.
 */
public class BarScalebarDrawer extends ScalebarDrawer {
    public BarScalebarDrawer(PDFCustomBlocks customBlocks, ScalebarBlock block, List<Label> labels, int barSize,
                             int labelDistance, int subIntervals, float intervalWidth, Font pdfFont,
                             float leftLabelMargin, float rightLabelMargin, float maxLabelWidth, float maxLabelHeight
    ) {
        super(customBlocks, block, labels, barSize, labelDistance, subIntervals, intervalWidth,
                pdfFont, leftLabelMargin, rightLabelMargin, maxLabelWidth, maxLabelHeight);
    }

    protected void drawBar(PdfContentByte dc) {
        float subIntervalWidth = intervalWidth / subIntervals;
        for (int i = 0; i < block.getIntervals() * subIntervals; ++i) {
            float pos = i * subIntervalWidth;
            final BaseColor color = i % 2 == 0 ? block.getBarBgColorVal() : block.getColorVal();
            if (color != null) {
                dc.setColorFill(color);
                dc.rectangle(pos, 0, subIntervalWidth, barSize);
                dc.fill();
            }
        }

        dc.rectangle(0, 0, intervalWidth * block.getIntervals(), barSize);
        dc.stroke();
    }
}
