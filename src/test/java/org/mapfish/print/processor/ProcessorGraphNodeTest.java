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
import org.mapfish.print.output.Values;

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
        processor.getInputMapper().put(iMappingName, "i");
        processor.getInputMapper().put(bMappingName, "b");
        DataTransferObject param = ProcessorGraphNode.populateInputParameter(processor, values);

        assertEquals(sVal, param.getS());
        assertEquals(intVal.intValue(), param.getI());
        assertEquals(true, param.isB());
        assertEquals(new DataTransferObject().getDefaultI(), param.getDefaultI());
        assertEquals(lsVal, param.getLs());
        assertArrayEquals(daVal, param.getDa(), 0.00001);
    }

    @Test
    public void testWriteProcessorOutputToValues() throws Exception {
        Values values = new Values();

        final DataTransferObject dto = new DataTransferObject();
        dto.setB(true);
        dto.setDa(daVal);
        dto.setDefaultI(32);
        dto.setLs(lsVal);

        TestProcessor processor = new TestProcessor();
        processor.getInputMapper().put(iMappingName, "i");
        processor.getInputMapper().put(bMappingName, "b");

        ProcessorGraphNode.writeProcessorOutputToValues(dto, processor.getOutputMapper(), values);

        assertEquals(dto.getDefaultI(), values.getInteger("defaultI").intValue());
        assertEquals(dto.getI(), values.getInteger(iMappingName).intValue());
        assertEquals(dto.isB(), values.getBoolean(bMappingName));
        assertNull(values.getBoolean("s"));
        assertEquals(dto.isB(), values.getBoolean(bMappingName));
        assertEquals(lsVal, values.getObject("ls", Object.class));
        assertArrayEquals(daVal, (double[]) values.getObject("da", Object.class), 0.00001);
    }

    static class DataTransferObject {
        int defaultI = 3;
        int i;
        boolean b;
        String s;
        List<String> ls;
        double[] da;

        public int getDefaultI() {
            return defaultI;
        }

        public void setDefaultI(int defaultI) {
            this.defaultI = defaultI;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public boolean isB() {
            return b;
        }

        public void setB(boolean b) {
            this.b = b;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        public List<String> getLs() {
            return ls;
        }

        public void setLs(List<String> ls) {
            this.ls = ls;
        }

        public double[] getDa() {
            return da;
        }

        public void setDa(double[] da) {
            this.da = da;
        }
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
        public DataTransferObject execute(DataTransferObject values) throws Exception {
            return null;
        }
    }
}
