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

package org.mapfish.print.scalebar;

import com.lowagie.text.pdf.BaseFont;

/**
 * Position, size and content of a label
 */
public class Label {
    /**
     * Position of the label, relative to the first tick of the bar.
     */
    public final float paperOffset;

    public String label;

    /**
     * size of the text in the axis of the bar.
     */
    public final float width;

    /**
     * size of the text in the perpendicular axis of the bar
     */
    public final float height;

    public Label(float paperOffset, String label, BaseFont font, double fontSize, boolean rotated) {
        this.paperOffset = paperOffset;
        this.label = label;
        final float textWidth = font.getWidthPoint(label, (float) fontSize);
        final float textHeight = font.getAscentPoint(label, (float) fontSize) - font.getDescentPoint(label, (float) fontSize);
        if (rotated) {
            this.height = textWidth;
            this.width = textHeight;
        } else {
            this.width = textWidth;
            this.height = textHeight;
        }
    }
}
