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

import com.lowagie.text.*;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import java.util.List;


/**
 * Holds the config of a page and knows how to print it.
 */
public class Page {
    protected List<Block> items;
    private String pageSize = "A4";
    private HeaderFooter header = null;
    private HeaderFooter footer = null;
    private String marginLeft = "40";
    private String marginRight = "40";
    private String marginTop = "20";
    private String marginBottom = "20";
    private String backgroundPdf = null;
    private boolean landscape = false;

    public void render(PJsonObject params, RenderingContext context) throws DocumentException {
        final Document doc = context.getDocument();
        doc.setPageSize(getPageSizeRect(context, params));
        doc.setMargins(getMarginLeft(context, params), getMarginRight(context, params),
                getMarginTop(context, params) + (header != null ? header.getHeight() : 0),
                getMarginBottom(context, params) + (footer != null ? footer.getHeight() : 0));

        context.getCustomBlocks().setBackgroundPdf(PDFUtils.evalString(context, params, backgroundPdf));
        if (doc.isOpen()) {
            doc.newPage();
        } else {
            doc.open();
        }
        context.getCustomBlocks().setHeader(header, params);
        context.getCustomBlocks().setFooter(footer, params);

        for (int i = 0; i < items.size(); i++) {
            Block block = items.get(i);
            if (block.isVisible(context, params)) {
                block.render(params, new Block.PdfElement() {
                    public void add(Element element) throws DocumentException {
                        doc.add(element);
                    }
                }, context);
            }
        }
    }

    public Rectangle getPageSizeRect(RenderingContext context, PJsonObject params) {
        final Rectangle result = PageSize.getRectangle(getPageSize(context, params));
        if (landscape) {
            return result.rotate();
        } else {
            return result;
        }
    }

    public List<Block> getItems() {
        return items;
    }

    public void setItems(List<Block> items) {
        this.items = items;
    }

    public String getPageSize(RenderingContext context, PJsonObject params) {
        return PDFUtils.evalString(context, params, pageSize);
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
        try {
            PageSize.getRectangle(pageSize);
        } catch (RuntimeException e) {
            throw new InvalidValueException("pageSize", pageSize);
        }
    }

    public void setHeader(HeaderFooter header) {
        this.header = header;
    }

    public void setFooter(HeaderFooter footer) {
        this.footer = footer;
    }

    public int getMarginLeft(RenderingContext context, PJsonObject params) {
        return Integer.parseInt(PDFUtils.evalString(context, params, marginLeft));
    }

    public int getMarginRight(RenderingContext context, PJsonObject params) {
        return Integer.parseInt(PDFUtils.evalString(context, params, marginRight));
    }

    public int getMarginTop(RenderingContext context, PJsonObject params) {
        return Integer.parseInt(PDFUtils.evalString(context, params, marginTop));
    }

    public int getMarginBottom(RenderingContext context, PJsonObject params) {
        return Integer.parseInt(PDFUtils.evalString(context, params, marginBottom));
    }

    public void setMarginLeft(String marginLeft) {
        this.marginLeft = marginLeft;
    }

    public void setMarginRight(String marginRight) {
        this.marginRight = marginRight;
    }

    public void setMarginTop(String marginTop) {
        this.marginTop = marginTop;
    }

    public void setMarginBottom(String marginBottom) {
        this.marginBottom = marginBottom;
    }

    public void setBackgroundPdf(String backgroundPdf) {
        this.backgroundPdf = backgroundPdf;
    }

    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    /**
     * Called just after the config has been loaded to check it is valid.
     *
     * @throws InvalidValueException When there is a problem
     */
    public void validate() {
        if (items == null) throw new InvalidValueException("items", "null");
        if (items.size() < 1) throw new InvalidValueException("items", "[]");
        for (int i = 0; i < items.size(); i++) {
            items.get(i).validate();
        }

        if (header != null) header.validate();
        if (footer != null) footer.validate();
    }
}
