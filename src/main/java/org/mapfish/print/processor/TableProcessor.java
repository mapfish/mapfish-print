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

package org.mapfish.print.processor;

import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.output.Values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A processor for generating a table.
 * 
 * @author Jesse
 */
public class TableProcessor extends AbstractProcessor {
    private String tableRef;

    @Override
	public final Map<String, Object> execute(final Values values) throws Exception {
        final Map<String, Object> output = new HashMap<String, Object>();
        final PJsonObject jsonTable = values.getObject(this.tableRef, PJsonObject.class);
        final List<Map<String, String>> table = new ArrayList<Map<String, String>>();

        final PJsonArray jsonColumns = jsonTable.getJSONArray("columns");
        final PJsonArray jsonData = jsonTable.getJSONArray("data");
        for (int i = 0; i < jsonData.size(); i++) {
            final PJsonArray jsonRow = jsonData.getJSONArray(i);
            final Map<String, String> row = new HashMap<String, String>();
            for (int j = 0; j < jsonRow.size(); j++) {
                row.put(jsonColumns.getString(j), jsonRow.getString(j));
            }
            table.add(row);
        }

        output.put("table", table);
        return output;
    }

    public final String getTableRef() {
        return this.tableRef;
    }

    public final void setTableRef(final String tableRef) {
        this.tableRef = tableRef;
    }
}
