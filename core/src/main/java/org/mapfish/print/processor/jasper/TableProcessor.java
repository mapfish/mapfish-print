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

import com.google.common.collect.Maps;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.mapfish.print.attribute.TableAttribute.TableAttributeValue;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.wrapper.json.PJsonArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A processor for generating a table.
 *
 * @author Jesse
 * @author sbrunner
 */
public final class TableProcessor extends AbstractProcessor<TableProcessor.Input, TableProcessor.Output> {
    private Map<String, TableColumnConverter> columnConverterMap = Maps.newHashMap();

    /**
     * Constructor.
     */
    protected TableProcessor() {
        super(Output.class);
    }

    /**
     * Set strategies for converting the textual representation of each column to some other object (image, other text, etc...).
     *
     * @param columnConverters Map from column name -> {@link TableColumnConverter}
     */
    public void setColumns(final Map<String, TableColumnConverter> columnConverters) {
        this.columnConverterMap = columnConverters;
    }

    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Override
    public Output execute(final Input values, final ExecutionContext context) throws Exception {
        final TableAttributeValue jsonTable = values.table;
        final Collection<Map<String, ?>> table = new ArrayList<Map<String, ?>>();

        final String[] jsonColumns = jsonTable.columns;
        final PJsonArray[] jsonData = jsonTable.data;
        for (final PJsonArray jsonRow : jsonData) {
            checkCancelState(context);
            final Map<String, Object> row = new HashMap<String, Object>();
            for (int j = 0; j < jsonRow.size(); j++) {
                final String columnName = jsonColumns[j];
                final String rowValue = jsonRow.getString(j);
                TableColumnConverter converter = this.columnConverterMap.get(columnName);
                if (converter != null) {
                    Object convertedValue = converter.resolve(rowValue);
                    row.put(columnName, convertedValue);
                } else {
                    row.put(columnName, rowValue);
                }
            }
            table.add(row);
        }

        final JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(table);
        return new Output(dataSource);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        // no checks needed
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
