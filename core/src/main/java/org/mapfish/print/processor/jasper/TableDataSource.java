/*
 * Copyright (C) 2014  Camptocamp
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
/**
 * @author Stéphane Brunner
 */

package org.mapfish.print.processor.jasper;

import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;

import javax.swing.table.AbstractTableModel;

/**
 * Processor that has data for a table in a jasper report.
 *
 * @author Jesse
 * @author sbrunner
 */
public class TableDataSource extends AbstractTableModel {

    private static final long serialVersionUID = -3012559112008645883L;
    private final String[] columnNames;
    private final Object[][] data;

    /**
     * Constructor.
     *
     * @param table the table data.
     */
    public TableDataSource(final PJsonObject table) {
        PJsonArray jsonColumns = table.getJSONArray("columns");
        this.columnNames = new String[jsonColumns.size()];
        for (int i = 0; i < jsonColumns.size(); i++) {
            this.columnNames[i] = jsonColumns.getString(i);
        }

        PJsonArray jsonData = table.getJSONArray("data");
        this.data = new String[jsonData.size()][];
        for (int i = 0; i < jsonData.size(); i++) {
            PJsonArray jsonRow = jsonData.getJSONArray(i);
            this.data[i] = new String[jsonRow.size()];
            for (int j = 0; j < jsonRow.size(); j++) {
                this.data[i][j] = jsonRow.getString(j);
            }
        }

    }


    /**
     * Constructor.
     *
     * @param columnNames the names of each column in the table
     * @param data the table data.
     */
    public TableDataSource(final String[] columnNames, final Object[][] data) {
        this.columnNames = columnNames;
        this.data = data;
    }

    @Override
    public final int getColumnCount() {
        return this.columnNames.length;
    }

    @Override
    public final String getColumnName(final int columnIndex) {
        return this.columnNames[columnIndex];
    }

    public final String[] getColumnNames() {
        return this.columnNames;
    }

    @Override
    public final int getRowCount() {
        return this.data.length;
    }

    @Override
    public final Object getValueAt(final int rowIndex, final int columnIndex) {
        return this.data[rowIndex][columnIndex];
    }
}
