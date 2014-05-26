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

import java.util.ArrayList;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPTableEvent;

/**
 * Base class for the chunk drawers. It's for parts of the page that needs direct
 * access to the PDF for rendering (usage of the {@link com.itextpdf.text.pdf.PdfContentByte}).
 * <p/>
 * Those are scheduled for rendering and will be invoqued ({@link #renderImpl(com.itextpdf.text.Rectangle, com.itextpdf.text.pdf.PdfContentByte)})
 * when iText is actually rendering the corresponding block.
 */
public abstract class ChunkDrawer implements PdfPTableEvent {
    private final List<PDFCustomBlocks.AbsoluteDrawer> others = new ArrayList<PDFCustomBlocks.AbsoluteDrawer>();
    private final PDFCustomBlocks customBlocks;

    public ChunkDrawer(PDFCustomBlocks customBlocks) {
        this.customBlocks = customBlocks;
    }

    public void tableLayout(PdfPTable table, float widths[][], float heights[], int headerRows, int rowStart, PdfContentByte[] canvases) {
        PdfContentByte dc = canvases[PdfPTable.LINECANVAS];
        Rectangle rect = new Rectangle(widths[0][0], heights[1], widths[0][1], heights[0]);
        render(rect, dc);
    }

    public final void render(Rectangle rectangle, PdfContentByte dc) {
        customBlocks.blockRendered(this);

        renderImpl(rectangle, dc);
        for (int i = 0; i < others.size(); i++) {
            PDFCustomBlocks.AbsoluteDrawer absoluteDrawer = others.get(i);
            try {
                absoluteDrawer.render(dc);
            } catch (DocumentException e) {
                customBlocks.addError(e);
            }
        }
    }

    public abstract void renderImpl(Rectangle rectangle, PdfContentByte dc);

    public void addAbsoluteDrawer(PDFCustomBlocks.AbsoluteDrawer chunkDrawer) {
        others.add(chunkDrawer);
    }
}
