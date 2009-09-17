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
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPTable;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import java.util.List;

/**
 * Bean to configure a !columns or a !table block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Columnsblock
 */
public class ColumnsBlock extends Block {
    private List<Block> items;
    private int[] widths = null;
    private int absoluteX = Integer.MIN_VALUE;
    private int absoluteY = Integer.MIN_VALUE;
    private int width = Integer.MIN_VALUE;
    private int nbColumns = Integer.MIN_VALUE;
    private TableConfig config = null;

    public void render(final PJsonObject params, PdfElement target, final RenderingContext context) throws DocumentException {

        if (isAbsolute()) {
            context.getCustomBlocks().addAbsoluteDrawer(new PDFCustomBlocks.AbsoluteDrawer() {
                public void render(PdfContentByte dc) throws DocumentException {
                    final PdfPTable table = PDFUtils.buildTable(items, params, context, nbColumns, config);
                    if (table != null) {
                        table.setTotalWidth(width);
                        table.setLockedWidth(true);

                        if (widths != null) {
                            table.setWidths(widths);
                        }

                        table.writeSelectedRows(0, -1, absoluteX, absoluteY, dc);
                    }
                }
            });
        } else {
            final PdfPTable table = PDFUtils.buildTable(items, params, context, nbColumns, config);
            if (table != null) {
                if (widths != null) {
                    table.setWidths(widths);
                }

                table.setSpacingAfter((float) spacingAfter);
                target.add(table);
            }
        }
    }

    public void setItems(List<Block> items) {
        this.items = items;
    }

    public void setWidths(int[] widths) {
        this.widths = widths;
    }

    public void setAbsoluteX(int absoluteX) {
        this.absoluteX = absoluteX;
    }

    public void setAbsoluteY(int absoluteY) {
        this.absoluteY = absoluteY;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setNbColumns(int nbColumns) {
        this.nbColumns = nbColumns;
    }

    public boolean isAbsolute() {
        return absoluteX != Integer.MIN_VALUE &&
                absoluteY != Integer.MIN_VALUE &&
                width != Integer.MIN_VALUE;
    }

    public MapBlock getMap() {
        for (Block item : items) {
            MapBlock result = item.getMap();
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public void setConfig(TableConfig config) {
        this.config = config;
    }

    public void validate() {
        super.validate();
        if (items == null) throw new InvalidValueException("items", "null");
        if (items.size() < 1) throw new InvalidValueException("items", "[]");

        if (!((absoluteX != Integer.MIN_VALUE && absoluteY != Integer.MIN_VALUE && width != Integer.MIN_VALUE) ||
                (absoluteX == Integer.MIN_VALUE && absoluteY == Integer.MIN_VALUE && width == Integer.MIN_VALUE))) {
            throw new InvalidValueException("absoluteX, absoluteY or width", "all of them must be defined or none");
        }

        for (int i = 0; i < items.size(); i++) {
            final Block item = items.get(i);
            item.validate();
            if (item.isAbsolute()) {
                throw new InvalidValueException("items", "Cannot put an absolute block in a !columns or !table block");
            }
        }

        if (config != null) config.validate();
    }
}