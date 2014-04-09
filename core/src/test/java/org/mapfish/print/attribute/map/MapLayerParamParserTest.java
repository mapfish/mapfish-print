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

package org.mapfish.print.attribute.map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.ExtraPropertyException;
import org.mapfish.print.MissingPropertyException;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.processor.HasDefaultValue;
import org.mapfish.print.processor.InputOutputValueUtils;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mapfish.print.AbstractMapfishSpringTest.parseJSONObjectFromFile;

/**
 * @author Jesse on 4/3/14.
 */
public class MapLayerParamParserTest {
    private final MapLayerParamParser mapLayerParamParser = new MapLayerParamParser();

    @Test
    public void testNullOptionalArray() throws Exception {
        OptionalArrayParam p = new OptionalArrayParam();

        final PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"sa\":null}");

        this.mapLayerParamParser.populateLayerParam(true, json, p, "toIgnore");
        assertNull(p.sa);
    }

    @Test
    public void testEnum() throws Exception {
        TestEnumParam p = new TestEnumParam();

        final PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"e1\":\"VAL1\", \"e2\": [1, \"VAL2\"]}");

        this.mapLayerParamParser.populateLayerParam(true, json, p, "toIgnore");
        assertEquals(TestEnum.DEF, p.def2);
        assertEquals(TestEnum.VAL1, p.e1);
        assertArrayEquals(new TestEnum[] {TestEnum.VAL2, TestEnum.VAL2}, p.e2);
    }

   @Test (expected = IllegalArgumentException.class)
    public void testEnumIllegalVal() throws Exception {
        TestEnumParam p = new TestEnumParam();

        final PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"e1\":\"foo\", \"e2\": [2, \"VAL2\"]}");

        this.mapLayerParamParser.populateLayerParam(true, json, p, "toIgnore");
    }

    @Test
    public void testPopulateLayerParam() throws Exception {
        final TestParam param = new TestParam();
        final PJsonObject json = parseJSONObjectFromFile(MapLayerParamParserTest.class, "mapAttributeTest.json");

        this.mapLayerParamParser.populateLayerParam(true, json, param, "toIgnore");

        assertEquals("string", param.s);
        assertEquals("newValue", param.defS);
        assertEquals("default2", param.defS2);
        assertEquals(11, param.bigI.intValue());
        assertEquals(1, param.littleI);
        final double delta = 0.00001;
        assertEquals(12.0, param.bigD, delta);
        assertEquals(2, param.littleD, delta);
        assertEquals(13.0f, param.bigF, delta);
        assertEquals(3.0f, param.littleF, delta);
        assertEquals(true, param.bigB);
        assertEquals(true, param.littleB);
        assertEquals("fieldValue", param.po.getString("poField"));
        assertEquals("http://localhost:8080", param.url.toExternalForm());

        assertEquals(3, param.pa.size());
        assertEquals(1, param.pa.getInt(0));
        assertEquals("hi", param.pa.getString(1));
        assertEquals(3, param.pa.getJSONObject(2).getInt("faField"));

        assertArrayEquals(new String[]{"s1", "s2"}, param.sa);

        assertEquals(2, param.ba.length);
        assertEquals(true, param.ba[0]);
        assertEquals(false, param.ba[1]);
        assertArrayEquals(new double[]{2.1, 2.2}, param.da, delta);


        assertEquals(2, param.oa.length);
        assertEquals(true, param.oa[0].has("f"));
        assertEquals(true, param.oa[1].has("b"));
        assertEquals(true, param.calledByPostConstruct);

        assertEquals("embeddedValue", param.e.embeddedValue);
        assertEquals("def", param.e.embeddedDefault);
        assertTrue(param.e.calledByPostConstruct);

        assertEquals(2, param.ea.length);

        assertEquals("embeddedValue2", param.ea[0].embeddedValue);
        assertEquals("updateddef", param.ea[0].embeddedDefault);
        assertTrue(param.ea[0].calledByPostConstruct);

        assertEquals("embeddedValue3", param.ea[1].embeddedValue);
        assertEquals("def", param.ea[1].embeddedDefault);
        assertTrue(param.ea[1].calledByPostConstruct);
    }



    @Test
    public void testPopulateLayerParam_MissingParam() throws Exception {
        final TestParam param = new TestParam();

        try {
            this.mapLayerParamParser.populateLayerParam(true, new PJsonObject(new JSONObject(), "spec"), param);
            fail("Expected an exception to be raised");
        } catch (MissingPropertyException e) {
            final Matcher missingParameterMatcher = Pattern.compile("\\*.+?").matcher(e.getMessage());
            int errorCount = 0;
            while (missingParameterMatcher.find()) {
                errorCount ++;
            }
            int totalAttributes = InputOutputValueUtils.getAllAttributeNames(param.getClass()).size();
            assertEquals(18 + totalAttributes, errorCount);

            assertEquals(18, e.getMissingProperties().size());
            assertEquals(totalAttributes, e.getAttributeNames().size());
        }

    }
    @Test
    public void testPopulateLayerParam_TooManyParams() throws Exception {
        final TestParam param = new TestParam();
        final PJsonObject json = parseJSONObjectFromFile(MapLayerParamParserTest.class, "mapAttributeTest.json");
        json.getInternalObj().put("extraProperty", "value");
        json.getInternalObj().put("extraProperty2", "value2");
        try {
            this.mapLayerParamParser.populateLayerParam(true, json, param, "toIgnore");
        } catch (ExtraPropertyException e) {
            final Matcher missingParameterMatcher = Pattern.compile("\\*.+?").matcher(e.getMessage());
            int errorCount = 0;
            while (missingParameterMatcher.find()) {
                errorCount ++;
            }
            int totalAttributes = InputOutputValueUtils.getAllAttributeNames(param.getClass()).size();
            assertEquals(2 + totalAttributes, errorCount);

            assertEquals(2, e.getExtraProperties().size());
            assertEquals(totalAttributes, e.getAttributeNames().size());
            assertTrue(e.getExtraProperties().contains("extraProperty"));
        }
    }

    public static void populateLayerParam(PJsonObject requestData, Object param, String... extraNamesToIgnore)
            throws IOException, JSONException {
        new MapLayerParamParser().populateLayerParam(true, requestData, param, extraNamesToIgnore);
    }

    static class TestParam {
        public String s;
        @HasDefaultValue
        public String defS = "default";
        @HasDefaultValue
        public String defS2 = "default2";
        public Integer bigI;
        public int littleI;
        public Double bigD;
        public double littleD;
        public Float bigF;
        public float littleF;
        public Boolean bigB;
        public boolean littleB;
        public PJsonObject po;
        public URL url;
        public PJsonArray pa;
        public String[] sa;
        public boolean[] ba;
        public PJsonObject[] oa;
        public double[] da;
        public EmbeddedClass e;
        public EmbeddedClass[] ea;

        private boolean calledByPostConstruct = false;

        public void postConstruct() {
            calledByPostConstruct = true;
        }
    }

    static class EmbeddedClass {
        public String embeddedValue;
        @HasDefaultValue
        public String embeddedDefault = "def";
        private boolean calledByPostConstruct = false;

        public void postConstruct() {
            calledByPostConstruct = true;
        }
    }

    static class OptionalArrayParam {
        @HasDefaultValue
        public String[] sa;
    }

    enum TestEnum {
        VAL1, VAL2, DEF
    }

    static class TestEnumParam {
        public TestEnum e1 = TestEnum.DEF;
        public TestEnum[] e2;
        @HasDefaultValue
        public TestEnum def2 = TestEnum.DEF;
    }
}
