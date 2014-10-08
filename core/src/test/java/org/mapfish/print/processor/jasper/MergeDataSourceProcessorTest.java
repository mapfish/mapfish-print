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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignField;
import org.junit.Test;
import org.mapfish.print.output.Values;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mapfish.print.processor.jasper.MergeDataSourceProcessor.In;
import static org.mapfish.print.processor.jasper.MergeDataSourceProcessor.Out;
import static org.mapfish.print.processor.jasper.MergeDataSourceProcessor.Source;
import static org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType.DATASOURCE;
import static org.mapfish.print.processor.jasper.MergeDataSourceProcessor.SourceType.SINGLE;

public class MergeDataSourceProcessorTest {

    @Test
    public void testExec() throws Exception {
        Values values = new Values();
        values.put("row1", "hello10");
        values.put("row11", "hello11");
        values.put("row2", "hello2");
        List<Map<String, ?>> innerData = Lists.newArrayList(
            createRow("r1val1", "r1val2", "r1val3"),
            createRow("r2val1", "r2val2", "r2val3"),
            createRow("r3val1", "r3val2", "r3val3")
        );
        JRMapCollectionDataSource datasource = new JRMapCollectionDataSource(innerData);
        values.put("manyRows", datasource);

        final MergeDataSourceProcessor processor = new MergeDataSourceProcessor();

        Map<String, String> fieldMap1 = Maps.newHashMap();
        fieldMap1.put("row1", "row1");
        fieldMap1.put("row11", "row11");
        Map<String, String> fieldMap = Maps.newHashMap();
        fieldMap.put("col1", "col1");
        fieldMap.put("col3", "col2");
        List<Source> source = Lists.newArrayList(
                Source.createSource(null, SINGLE, fieldMap1),
                Source.createSource(null, SINGLE, Collections.singletonMap("row2", "renamed")),
                Source.createSource("manyRows", DATASOURCE, fieldMap));
        processor.setSources(source);

        List<Throwable> errors = Lists.newArrayList();
        processor.validate(errors, null);

        assertEquals(errors.toString(), 0, errors.size());

        In in = new In();
        in.values = values;
        final Out execute = processor.execute(in, null);

        JRDesignField field = new JRDesignField();
        field.setName("row1");
        assertTrue(execute.mergedDataSource.next());
        Object value = execute.mergedDataSource.getFieldValue(field);
        assertEquals("hello10", value);
        field.setName("row11");
        value = execute.mergedDataSource.getFieldValue(field);
        assertEquals("hello11", value);

        assertTrue(execute.mergedDataSource.next());
        field.setName("renamed");
        value = execute.mergedDataSource.getFieldValue(field);
        assertEquals("hello2", value);

        for (int i = 0; i < 3 ; i++) {
            assertTrue(execute.mergedDataSource.next());
            field.setName("col1");
            value = execute.mergedDataSource.getFieldValue(field);
            assertEquals(innerData.get(i).get("col1"), value);

            field.setName("col3");
            value = execute.mergedDataSource.getFieldValue(field);
            assertNull(value);

            field.setName("col2");
            value = execute.mergedDataSource.getFieldValue(field);
            assertEquals(innerData.get(i).get("col3"), value);
        }
    }

    private Map<String, ?> createRow(String val1, String val2, String val3) {
        final HashMap<String, Object> map = Maps.newHashMap();
        map.put("col1", val1);
        map.put("col2", val2);
        map.put("col3", val3);

        return map;
    }
}