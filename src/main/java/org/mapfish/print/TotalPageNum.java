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

package org.mapfish.print;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * A class for handling the tricky task of displaying the total number of pages
 * on each page of the document.
 */
public class TotalPageNum {
    private static final String SAMPLE_VALUE = "999";

    private PdfTemplate totalPageNum;
    private final BaseFont totalPageNumFont;
    private final float totalPageNumFontSize;
    private PdfContentByte dc;

    public TotalPageNum(PdfWriter writer, Font font) {
        dc = writer.getDirectContent();
        totalPageNumFont = font.getCalculatedBaseFont(false);
        totalPageNumFontSize = font.getSize();
    }

    public Chunk createPlaceHolder() throws BadElementException {
        float width = totalPageNumFont.getWidthPoint(SAMPLE_VALUE, totalPageNumFontSize);
        float height = totalPageNumFont.getAscentPoint(SAMPLE_VALUE, totalPageNumFontSize) -
                totalPageNumFont.getDescentPoint(SAMPLE_VALUE, totalPageNumFontSize);
        if (totalPageNum == null) {
            totalPageNum = dc.createTemplate(width, height);
        }

        Image image = Image.getInstance(totalPageNum);
        return new Chunk(image, 0, 0, true);

    }

    public void render(PdfWriter writer) {
        totalPageNum.beginText();
        totalPageNum.setFontAndSize(totalPageNumFont, totalPageNumFontSize);
        totalPageNum.setTextMatrix(0, 0);
        totalPageNum.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPageNum.endText();
    }
}
