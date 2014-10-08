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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.HasDefaultValue;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Jesse on 3/28/14.
 */
public class ProcessorGraphNodeTest {
    final String iMappingName = "integer";
    Integer intVal = 1;
    final String bMappingName = "bool";
    String sVal = "sValue";
    ArrayList<String> lsVal = Lists.newArrayList("one", "two");
    double[] daVal = new double[] {1.2, 2.3};

    @Test
    public void testPopulateInputParameter() throws Exception {

        Values values = new Values();
        values.put(iMappingName, intVal);
        values.put(bMappingName, true);
        values.put("s", sVal);
        values.put("ls", lsVal);
        values.put("da", daVal);

        TestProcessor processor = new TestProcessor();
        processor.getInputMapperBiMap().put(iMappingName, "i");
        processor.getInputMapperBiMap().put(bMappingName, "b");
        DataTransferObject param = ProcessorUtils.populateInputParameter(processor, values);

        assertEquals(sVal, param.s);
        assertEquals(intVal.intValue(), param.i);
        assertEquals(true, param.b);
        assertEquals(new DataTransferObject().defaultI, param.defaultI);
        assertEquals(lsVal, param.ls);
        assertArrayEquals(daVal, param.da, 0.00001);
    }

    @Test (expected = RuntimeException.class)
    public void testNullableProperty() throws Exception {

        Values values = new Values();
        values.put(iMappingName, intVal);
        values.put(bMappingName, true);
        values.put("s", sVal);
        values.put("ls", lsVal);
        // NO da value is specified so an exception should be thrown.

        TestProcessor processor = new TestProcessor();
        processor.getInputMapperBiMap().put(iMappingName, "i");
        processor.getInputMapperBiMap().put(bMappingName, "b");
        ProcessorUtils.populateInputParameter(processor, values);


    }

    @Test
    public void testWriteProcessorOutputToValues() throws Exception {
        Values values = new Values();

        final DataTransferObject dto = new DataTransferObject();
        dto.b = true;
        dto.da = daVal;
        dto.defaultI = 32;
        dto.ls = lsVal;

        TestProcessor processor = new TestProcessor();
        processor.getOutputMapperBiMap().put("i", iMappingName);
        processor.getOutputMapperBiMap().put("b", bMappingName);

        ProcessorUtils.writeProcessorOutputToValues(dto, processor, values);

        assertEquals(dto.defaultI, values.getInteger("defaultI").intValue());
        assertEquals(dto.i, values.getInteger(iMappingName).intValue());
        assertEquals(dto.b, values.getBoolean(bMappingName));
        assertNull(values.getBoolean("s"));
        assertEquals(lsVal, values.getObject("ls", Object.class));
        assertArrayEquals(daVal, (double[]) values.getObject("da", Object.class), 0.00001);
    }

    @Test
    public void testWritePrefixedOutputToValues() throws Exception {
        Values values = new Values();

        final DataTransferObject dto = new DataTransferObject();
        dto.b = true;
        dto.da = daVal;
        dto.defaultI = 32;
        dto.ls = lsVal;


        TestProcessor processor = new TestProcessor();
        processor.getOutputMapperBiMap().put("i", iMappingName);
        processor.getOutputMapperBiMap().put("b", bMappingName);

        processor.setOutputPrefix("   prefix   ");
        ProcessorUtils.writeProcessorOutputToValues(dto, processor, values);

        assertEquals(dto.defaultI, values.getInteger("prefixDefaultI").intValue());
        assertEquals(dto.i, values.getInteger(iMappingName).intValue());
        assertEquals(dto.b, values.getBoolean(bMappingName));
        assertNull(values.getBoolean("prefixS"));
        assertEquals(lsVal, values.getObject("prefixLs", Object.class));
        assertArrayEquals(daVal, (double[]) values.getObject("prefixDa", Object.class), 0.00001);
    }

    static class DataTransferObject {
        @HasDefaultValue
        public int defaultI = 3;
        public int i;
        public boolean b;
        public String s;
        public List<String> ls;
        public double[] da;
    }

    static class TestProcessor extends AbstractProcessor<DataTransferObject, DataTransferObject> {

        /**
         * Constructor.
         */
        protected TestProcessor() {
            super(DataTransferObject.class);
        }

        @Override
        public DataTransferObject createInputParameter() {
            return new DataTransferObject();
        }

        @Nullable
        @Override
        public DataTransferObject execute(DataTransferObject values, ExecutionContext context) throws Exception {
            return null;
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
            // no checks
        }
    }
}
