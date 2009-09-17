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

package org.mapfish.print;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.apache.log4j.Logger;
import org.mapfish.print.config.layout.HeaderFooter;
import org.mapfish.print.utils.PJsonObject;

import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Listen to events from the PDF document in order to render the
 * custom {@link org.mapfish.print.ChunkDrawer}s, the header/footer and the background.
 */
public class PDFCustomBlocks extends PdfPageEventHelper {
    public static final Logger LOGGER = Logger.getLogger(PDFCustomBlocks.class);

    private ChunkDrawer last = null;
    private final PdfWriter writer;
    private final RenderingContext context;
    private HeaderFooter header;
    private PJsonObject headerParams;
    private HeaderFooter footer;
    private PJsonObject footerParams;
    private String backgroundPdf;
    private final List<Exception> errors = Collections.synchronizedList(new ArrayList<Exception>());

    /**
     * cache of background PDF pages
     */
    private final Map<String, PdfImportedPage> backgroundPdfs = new HashMap<String, PdfImportedPage>();

    /**
     * block for rendering the totalpage number.
     */
    private TotalPageNum totalPageNum = null;

    public PDFCustomBlocks(PdfWriter writer, RenderingContext context) {
        this.writer = writer;
        this.context = context;
        writer.setPageEvent(this);
    }

    public void onStartPage(PdfWriter writer, Document document) {
        super.onStartPage(writer, document);

        final PdfContentByte dc = writer.getDirectContent();
        addBackground(writer, document, dc);
    }

    public void onEndPage(PdfWriter writer, Document document) {
        final PdfContentByte dc = writer.getDirectContent();
        addHeader(document, dc);
        addFooter(document, dc);
        addErrors(writer);
        super.onEndPage(writer, document);
    }

    public void onCloseDocument(PdfWriter writer, Document document) {
        if (totalPageNum != null) {
            totalPageNum.render(writer);
        }
        super.onCloseDocument(writer, document);
    }

    private void addBackground(PdfWriter writer, Document document, PdfContentByte dc) {
        if (backgroundPdf != null) {
            try {
                PdfImportedPage page = backgroundPdfs.get(backgroundPdf);
                if (page == null) {
                    PdfReader reader = new PdfReader(backgroundPdf);
                    page = writer.getImportedPage(reader, 1);
                    backgroundPdfs.put(backgroundPdf, page);
                }
                final Rectangle pageSize = document.getPageSize();
                final boolean rotate = (page.getWidth() < page.getHeight()) ^ (pageSize.getWidth() < pageSize.getHeight());
                if (rotate) {
                    dc.addTemplate(page, 0, -1, 1, 0, 0, pageSize.getHeight());
                } else {
                    dc.addTemplate(page, 0, 0);
                }
            } catch (IOException e) {
                addError(e);
            }
        }
    }

    private void addHeader(Document document, PdfContentByte dc) {
        if (header != null) {
            Rectangle rectangle = new Rectangle(document.left(), document.top(),
                    document.right(), document.top() + header.getHeight());
            header.render(rectangle, dc, headerParams, context);
        }
    }

    private void addFooter(Document document, PdfContentByte dc) {
        if (footer != null) {
            Rectangle rectangle = new Rectangle(document.left(), document.bottom() - footer.getHeight(), document.right(), document.bottom());
            footer.render(rectangle, dc, footerParams, context);
        }
    }

    private void addErrors(PdfWriter writer) {
        if (errors.size() > 0) {
            StringBuilder errorTxt = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                Exception exception = errors.get(i);
                errorTxt.append(exception).append("\n");
            }
            errors.clear();

            final Rectangle rect = new Rectangle(20f, 40f, 40f, 60f);

            final PdfAnnotation annotation = PdfAnnotation.createText(writer, rect, "Error", errorTxt.toString(), false, "Note");
            writer.addAnnotation(annotation);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Added an annotation for errors");
            }
        }
    }

    /**
     * Register a custom drawer.
     */
    public void addChunkDrawer(ChunkDrawer chunkDrawer) {
        last = chunkDrawer;
    }

    /**
     * Called when a custom drawer has been rendered.
     */
    public void blockRendered(ChunkDrawer chunkDrawer) {
        if (last == chunkDrawer) {
            last = null;
        }
    }

    /**
     * Schedule a absolute block (like a !columns or a !map).
     */
    public void addAbsoluteDrawer(AbsoluteDrawer chunkDrawer) throws DocumentException {
        if (last != null) {
            //a chunk drawer is scheduled, need to draw oneself after it.
            last.addAbsoluteDrawer(chunkDrawer);
        } else {
            //no chunk drawer is scheduled. We can draw it right away.
            chunkDrawer.render(writer.getDirectContent());
        }
    }

    public void setHeader(HeaderFooter header, PJsonObject params) {
        this.header = header;
        this.headerParams = params;
    }

    public void setFooter(HeaderFooter footer, PJsonObject params) {
        this.footer = footer;
        this.footerParams = params;
    }

    public void setBackgroundPdf(String backgroundPdf) {
        this.backgroundPdf = backgroundPdf;
    }

    public void addError(Exception e) {
        errors.add(e);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.warn("Error while adding a PDF element", e);
        } else {
            LOGGER.warn("Error while adding a PDF element" + e.toString());
        }
    }

    public Chunk getOrCreateTotalPagesBlock(Font font) throws BadElementException {
        if (totalPageNum == null) {
            totalPageNum = new TotalPageNum(writer, font);
        }

        return totalPageNum.createPlaceHolder();
    }

    /**
     * Base class for the absolute drawers
     */
    public static abstract class AbsoluteDrawer {
        public abstract void render(PdfContentByte dc) throws DocumentException;
    }

}
