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
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

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

        final Phrase text = PDFUtils.renderString(context, params, this.text, pdfFont);
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
