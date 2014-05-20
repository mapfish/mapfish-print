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

import java.util.List;

import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.config.layout.ScalebarBlock;

import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfContentByte;

/**
 * Draw a simple line with ticks.
 */
public class LineScalebarDrawer extends ScalebarDrawer {
    public LineScalebarDrawer(PDFCustomBlocks customBlocks, ScalebarBlock block, List<Label> labels, int barSize,
                              int labelDistance, int subIntervals, float intervalWidth, Font pdfFont,
                              float leftLabelMargin, float rightLabelMargin, float maxLabelWidth,
                              float maxLabelHeight) {
        super(customBlocks, block, labels, barSize, labelDistance, subIntervals, intervalWidth,
                pdfFont, leftLabelMargin, rightLabelMargin, maxLabelWidth, maxLabelHeight);
    }

    protected void drawBar(PdfContentByte dc) {
        dc.moveTo(0, 0);
        dc.lineTo(0, barSize);
        dc.lineTo(intervalWidth * block.getIntervals(), barSize);
        dc.lineTo(intervalWidth * block.getIntervals(), 0);
        for (int i = 0; i < block.getIntervals(); ++i) {
            float pos = i * intervalWidth;
            if (i > 0) {
                dc.moveTo(pos, 0);
                dc.lineTo(pos, barSize);
            }
            for (int j = 1; j < subIntervals; ++j) {
                pos += intervalWidth / subIntervals;
                dc.moveTo(pos, barSize);
                dc.lineTo(pos, (float)barSize / 2);
            }
        }
        dc.stroke();
    }
}
