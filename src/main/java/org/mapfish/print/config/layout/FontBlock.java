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

package org.mapfish.print.config.layout;

import com.itextpdf.text.BaseColor;
import java.awt.Color;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.config.ColorWrapper;

import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;

/**
 * Base class for block having font specifications
 */
public abstract class FontBlock extends Block {
    private String font = "Helvetica";
    protected Double fontSize = null;
    private String fontEncoding = BaseFont.WINANSI;

    private String fontColor = "black";

    public void setFont(String font) {
        this.font = font;
    }

    public void setFontSize(Double fontSize) {
        this.fontSize = fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
        if (fontSize < 0.0) throw new InvalidValueException("fontSize", fontSize);
    }

    public String getFont() {
        return font;
    }

    public double getFontSize() {
        if (fontSize != null) {
            return fontSize;
        } else {
            return 12.0;
        }
    }

    public void setFontEncoding(String fontEncoding) {
        this.fontEncoding = fontEncoding;
    }

    protected Font getPdfFont() {
        Font result = FontFactory.getFont(font, fontEncoding, (float) getFontSize());
        result.setColor(getFontColorVal());
        return result;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public BaseColor getFontColorVal() {
        return ColorWrapper.convertColor(fontColor);
    }
}