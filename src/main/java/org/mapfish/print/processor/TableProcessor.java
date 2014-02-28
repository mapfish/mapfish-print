package org.mapfish.print.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.output.Values;

public class TableProcessor extends AbstractProsessor {
    private String tableRef; 

    @Override
    public Map<String, Object> doProcess(Values values) throws Exception {
        final Map<String, Object> output = new HashMap<String, Object>();
        final PJsonObject jsonTable = (PJsonObject)values.getObject(tableRef);
        final List<Map<String, String>> table = new ArrayList<Map<String, String>>();
        
        final PJsonArray jsonColumns = jsonTable.getJSONArray("columns");
        final PJsonArray jsonData = jsonTable.getJSONArray("data");
        for (int i = 0; i < jsonData.size(); i++) {
            final PJsonArray jsonRow = jsonData.getJSONArray(i);
            final Map<String, String> row = new HashMap<String, String>();
            for (int j = 0; j < jsonRow.size(); j++) 
            {
                row.put(jsonColumns.getString(j), jsonRow.getString(j));
            }
            table.add(row);
        }

        output.put("table", table);
        return output;
    }

    public String getTableRef() {
        return tableRef;
    }

    public void setTableRef(String tableRef) {
        this.tableRef = tableRef;
    }
}
