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
/**
 * @author Stéphane Brunner
 */

package org.mapfish.print.processor.jasper;

import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;

import javax.swing.table.AbstractTableModel;

public class TableDataSource extends AbstractTableModel {

    private static final long serialVersionUID = -3012559112008645883L;
    private final String[] columnNames;
    private final Object[][] data;

    /**
     *
     */
    public TableDataSource(PJsonObject table) {
        PJsonArray jsonColumns = table.getJSONArray("columns");
        columnNames = new String[jsonColumns.size()];
        for (int i = 0; i < jsonColumns.size(); i++) {
            columnNames[i] = jsonColumns.getString(i);
        }

        PJsonArray jsonData = table.getJSONArray("data");
        data = new String[jsonData.size()][];
        for (int i = 0; i < jsonData.size(); i++) {
            PJsonArray jsonRow = jsonData.getJSONArray(i);
            data[i] = new String[jsonRow.size()];
            for (int j = 0; j < jsonRow.size(); j++) {
                data[i][j] = jsonRow.getString(j);
            }
        }

    }

    /**
     *
     */
    public TableDataSource(String[] columnNames, Object[][] data) {
        this.columnNames = columnNames;
        this.data = data;
    }

    /**
     *
     */
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     *
     */
    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    /**
     *
     */
    public String[] getColumnNames() {
        return columnNames;
    }

    /**
     *
     */
    @Override
    public int getRowCount() {
        return data.length;
    }

    /**
     *
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }
}
