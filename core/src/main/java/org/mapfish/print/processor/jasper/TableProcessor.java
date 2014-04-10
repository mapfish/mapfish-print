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

package org.mapfish.print.processor.jasper;

import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.mapfish.print.attribute.TableAttribute.TableAttributeValue;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.processor.AbstractProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A processor for generating a table.
 *
 * @author Jesse
 * @author sbrunner
 */
public class TableProcessor extends AbstractProcessor<TableProcessor.Input, TableProcessor.Output> {
    private static final String JSON_COLUMNS = "columns";
    private static final String JSON_DATA = "data";

    /**
     * Constructor.
     */
    protected TableProcessor() {
        super(Output.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values) throws Exception {
        final TableAttributeValue jsonTable = values.table;
        final Collection<Map<String, ?>> table = new ArrayList<Map<String, ?>>();

        final String[] jsonColumns = jsonTable.columns;
        final PJsonArray[] jsonData = jsonTable.data;
        for (int i = 0; i < jsonData.length; i++) {
            final PJsonArray jsonRow = jsonData[i];
            final Map<String, String> row = new HashMap<String, String>();
            for (int j = 0; j < jsonRow.size(); j++) {
                row.put(jsonColumns[j], jsonRow.getString(j));
            }
            table.add(row);
        }

        final JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(table);
        return new Output(dataSource);
    }

    /**
     * Input object for execute.
     */
    public static final class Input {
        /**
         * Data for constructing the table Datasource.
         */
        public TableAttributeValue table;
    }

    /**
     * The Output of the processor.
     */
    public static final class Output {
        /**
         * The table datasource.
         */
        public final JRMapCollectionDataSource table;

        private Output(final JRMapCollectionDataSource dataSource) {
            this.table = dataSource;
        }
    }
}
