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

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;

/**
 * Bean to configure a !text block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Textblock
 */
public class TextBlock extends FontBlock {
    private String text = "";

    public void render(PJsonObject params, PdfElement target, final RenderingContext context) throws DocumentException {
        Paragraph paragraph = new Paragraph();

        final Font pdfFont = getPdfFont();
        paragraph.setFont(pdfFont);

        final Phrase text = PDFUtils.renderString(context, params, this.text, pdfFont, null);
        paragraph.add(text);

        if (getAlign() != null) paragraph.setAlignment(getAlign().getCode());
        paragraph.setSpacingAfter((float) spacingAfter);
        target.add(paragraph);
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void validate() {
        super.validate();
        if (text == null) throw new InvalidValueException("text", "null");
    }
}
