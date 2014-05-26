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

import java.util.ArrayList;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * Config and logic to render a header or a footer.
 */
public class HeaderFooter {
    private int height = 0;
    private ArrayList<Block> items = new ArrayList<Block>();

    public void setHeight(int height) {
        this.height = height;
    }

    public void setItems(ArrayList<Block> items) {
        this.items = items;
    }

    public int getHeight() {
        return height;
    }

    public void render(final Rectangle rectangle, PdfContentByte dc, PJsonObject params, RenderingContext context) {
        try {
            final PdfPTable table = PDFUtils.buildTable(items, params, context, 1/*multiple items are arranged by lines*/, null);
            if (table != null) {
                table.setTotalWidth(rectangle.getWidth());
                table.writeSelectedRows(0, -1, rectangle.getLeft(), rectangle.getTop(), dc);
            }
        } catch (DocumentException e) {
            context.addError(e);
        }
    }

    public void validate() {
        if (height <= 0) throw new InvalidValueException("height", height);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).validate();
        }
    }
}
