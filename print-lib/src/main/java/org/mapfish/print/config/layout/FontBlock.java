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

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.InvalidValueException;

import java.awt.*;

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

    public Color getFontColorVal() {
        return ColorWrapper.convertColor(fontColor);
    }
}