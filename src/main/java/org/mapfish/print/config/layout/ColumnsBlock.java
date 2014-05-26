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

import java.util.List;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * Bean to configure a !columns or a !table block.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Columnsblock
 */
public class ColumnsBlock extends Block {
    private List<Block> items;
    private int[] widths = null;
    /*
    private int absoluteX = Integer.MIN_VALUE;
    private int absoluteY = Integer.MIN_VALUE;
    private int width = Integer.MIN_VALUE;
    */
    private double absoluteX = Double.MIN_VALUE;
    private double absoluteY = Double.MIN_VALUE;
    private double width = Double.MIN_VALUE;
    private int nbColumns = Integer.MIN_VALUE;
    private TableConfig config = null;

    public void render(final PJsonObject params, PdfElement target, final RenderingContext context) throws DocumentException {

        if (isAbsolute()) {
            context.getCustomBlocks().addAbsoluteDrawer(new PDFCustomBlocks.AbsoluteDrawer() {
                public void render(PdfContentByte dc) throws DocumentException {
                    final PdfPTable table = PDFUtils.buildTable(items, params, context, nbColumns, config);
                    if (table != null) {
                        table.setTotalWidth((float) width);
                        table.setLockedWidth(true);

                        if (widths != null) {
                            table.setWidths(widths);
                        }

                        table.writeSelectedRows(0, -1, (float) absoluteX, (float) absoluteY, dc);
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

    public void setAbsoluteX(double absoluteX) {
        this.absoluteX = absoluteX;
    }

    public void setAbsoluteY(double absoluteY) {
        this.absoluteY = absoluteY;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setNbColumns(int nbColumns) {
        this.nbColumns = nbColumns;
    }

    public boolean isAbsolute() {
        return absoluteX != Double.MIN_VALUE &&
                absoluteY != Double.MIN_VALUE &&
                width != Double.MIN_VALUE;
    }

    public MapBlock getMap(String name) {
        for (Block item : items) {
            MapBlock result = item.getMap(name);
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

        if (!((absoluteX != Double.MIN_VALUE && absoluteY != Double.MIN_VALUE && width != Double.MIN_VALUE) ||
                (absoluteX == Double.MIN_VALUE && absoluteY == Double.MIN_VALUE && width == Double.MIN_VALUE))) {
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