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
import com.lowagie.text.pdf.PdfPTable;
import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean to configure an !attributes block
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Attributesblock
 */
public class AttributesBlock extends Block {
    private String source;
    private ColumnDefs columnDefs = new ColumnDefs();

    private TableConfig tableConfig = null;

    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        PJsonObject sourceJson = params.optJSONObject(source);
        if (sourceJson == null) {
            sourceJson = context.getGlobalParams().optJSONObject(source);
        }
        if (sourceJson == null || sourceJson.size() == 0) {
            return;
        }
        PJsonArray data = sourceJson.optJSONArray("data");
        PJsonArray firstLine = sourceJson.getJSONArray("columns");

        final List<Integer> columnWidths;
        if (columnDefs.values().iterator().next().getColumnWeight() > 0) {
            columnWidths = new ArrayList<Integer>();
        } else {
            columnWidths = null;
        }

        //Compute the actual number of columns
        int nbCols = 0;
        for (int colNum = 0; colNum < firstLine.size(); ++colNum) {
            String name = firstLine.getString(colNum);
            ColumnDef colDef = columnDefs.get(name);
            if (colDef != null && colDef.isVisible(context, params)) {
                nbCols++;
                if (columnWidths != null) {
                    columnWidths.add(colDef.getColumnWeight());
                }
            } else {
                //noinspection ThrowableInstanceNeverThrown
                context.addError(new InvalidJsonValueException(firstLine, name, "Unknown column"));
            }
        }

        final PdfPTable table = new PdfPTable(nbCols);
        table.setWidthPercentage(100f);

        //deal with the weigths for the column widths, if specified 
        if (columnWidths != null) {
            int[] array = new int[columnWidths.size()];
            for (int i = 0; i < columnWidths.size(); i++) {
                array[i] = columnWidths.get(i);
            }
            table.setWidths(array);
        }

        //add the header
        int nbRows = data.size() + 1;
        for (int colNum = 0; colNum < firstLine.size(); ++colNum) {
            String name = firstLine.getString(colNum);
            ColumnDef colDef = columnDefs.get(name);
            if (colDef != null && colDef.isVisible(context, params)) {
                table.addCell(colDef.createHeaderPdfCell(params, context, colNum, nbRows, nbCols, tableConfig));
            }
        }
        table.setHeaderRows(1);

        //add the content
        for (int rowNum = 0; rowNum < data.size(); ++rowNum) {
            PJsonObject row = data.getJSONObject(rowNum);
            int realColNum = 0;
            for (int colNum = 0; colNum < firstLine.size(); ++colNum) {
                String name = firstLine.getString(colNum);
                ColumnDef colDef = columnDefs.get(name);
                if (colDef != null && colDef.isVisible(context, params)) {
                    table.addCell(colDef.createContentPdfCell(row, context, rowNum + 1, realColNum, nbRows, nbCols, tableConfig));
                    realColNum++;
                }
            }
        }
        table.setSpacingAfter((float) spacingAfter);

        target.add(table);
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setColumnDefs(ColumnDefs columnDefs) {
        this.columnDefs = columnDefs;
    }

    public void setTableConfig(TableConfig tableConfig) {
        this.tableConfig = tableConfig;
    }

    @Override
    public void validate() {
        super.validate();
        if (source == null) throw new InvalidValueException("source", "null");
        if (columnDefs == null)
            throw new InvalidValueException("columnDefs", "null");
        columnDefs.validate();
        if (tableConfig != null) tableConfig.validate();
    }
}
