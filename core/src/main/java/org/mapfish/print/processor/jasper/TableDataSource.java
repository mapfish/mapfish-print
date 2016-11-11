package org.mapfish.print.processor.jasper;

import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;

import javax.swing.table.AbstractTableModel;

/**
 * Processor that has data for a table in a jasper report.
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
