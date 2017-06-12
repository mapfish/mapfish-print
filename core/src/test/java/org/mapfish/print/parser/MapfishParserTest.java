package org.mapfish.print.parser;

import com.vividsolutions.jts.util.AssertionFailedException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.ExtraPropertyException;
import org.mapfish.print.MissingPropertyException;
import org.mapfish.print.attribute.LegendAttribute;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;

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

public class MapfishParserTest {
    private final MapfishParser mapfishJsonParser = new MapfishParser();

    @Test
    public void testNullOptionalArray() throws Exception {
        OptionalArrayParam p = new OptionalArrayParam();

        final PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"sa\":null}");

        this.mapfishJsonParser.parse(true, json, p, "toIgnore");
        assertNull(p.sa);
    }

    @Test
    public void testEnum() throws Exception {
        TestEnumParam p = new TestEnumParam();

        final PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"e1\":\"VAL1\", \"e2\": [1, \"VAL2\"]}");

        this.mapfishJsonParser.parse(true, json, p, "toIgnore");
        assertEquals(TestEnum.DEF, p.def2);
        assertEquals(TestEnum.VAL1, p.e1);
        assertArrayEquals(new TestEnum[]{TestEnum.VAL2, TestEnum.VAL2}, p.e2);
    }

    @Test
    public void testChoice() throws Exception {
        TestChoiceClass p = new TestChoiceClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"choiceA\":1}");

        String[] ignore = null; // done this way to remove compiler warning.
        this.mapfishJsonParser.parse(true, json, p, ignore);
        assertEquals(1, p.choiceA);
        assertEquals(0.0, p.choiceB, 0.0000000001);
        assertEquals(0, p.choiceC);

        p = new TestChoiceClass();
        json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"choiceB\":2.0}");
        this.mapfishJsonParser.parse(true, json, p);
        assertEquals(0, p.choiceA);
        assertEquals(2.0, p.choiceB, 0.0000000001);
        assertEquals(0, p.choiceC);

        p = new TestChoiceClass();
        json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"choiceC\":2}");
        this.mapfishJsonParser.parse(true, json, p);
        assertEquals(0, p.choiceA);
        assertEquals(0.0, p.choiceB, 0.0000000001);
        assertEquals(2, p.choiceC, 0.0000000001);

        p = new TestChoiceClass();
        json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"choiceA\":3,\"choiceC\":2}");
        this.mapfishJsonParser.parse(true, json, p);
        assertEquals(3, p.choiceA);
        assertEquals(0.0, p.choiceB, 0.0000000001);
        assertEquals(2, p.choiceC, 0.0000000001);

        p = new TestChoiceClass();
        json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"choiceB\":3.0,\"choiceC\":2}");
        this.mapfishJsonParser.parse(true, json, p);
        assertEquals(0, p.choiceA);
        assertEquals(3.0, p.choiceB, 0.0000000001);
        assertEquals(2, p.choiceC, 0.0000000001);
    }

    @Test(expected = AssertionFailedException.class)
    public void testBothOneOfChoices() throws Exception {
        TestChoiceClass p = new TestChoiceClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"choiceA\":1,\"choiceB\":2.0}");
        this.mapfishJsonParser.parse(true, json, p);
    }

    @Test(expected = AssertionFailedException.class)
    public void testBothOneOfChoicesAndCanSatisfyOneOf() throws Exception {
        TestChoiceClass p = new TestChoiceClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"choiceA\":1,\"choiceB\":2.0,\"choiceC\":3.0}");
        this.mapfishJsonParser.parse(true, json, p);
    }

    @Test(expected = AssertionFailedException.class)
    public void testMissingOneOfChoices() throws Exception {
        TestChoiceClass p = new TestChoiceClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{}");
        this.mapfishJsonParser.parse(true, json, p);
    }

    @Test
    public void testRequireDependantNotDefined() throws Exception {
        TestRequireClass p = new TestRequireClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{}");
        this.mapfishJsonParser.parse(true, json, p);
        assertNull(p.i);
        assertNull(p.b);
        assertNull(p.c);
    }

    @Test
    public void testRequirementSatisfied() throws Exception {
        TestRequireClass p = new TestRequireClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"i\":1, \"b\":true, \"c\":true}");
        this.mapfishJsonParser.parse(true, json, p);
        assertEquals(1, p.i.intValue());
        assertTrue(p.b);
        assertTrue(p.c);
    }

    @Test
    public void testOnlyRequirementDefinedNotDependant() throws Exception {
        TestRequireClass p = new TestRequireClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"b\":true}");
        this.mapfishJsonParser.parse(true, json, p);
        assertNull(p.i);
        assertTrue(p.b);
        assertNull(p.c);
    }

    @Test(expected = AssertionFailedException.class)
    public void testMissingBothRequirements() throws Exception {
        TestRequireClass p = new TestRequireClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"i\":1}");
        this.mapfishJsonParser.parse(true, json, p);
    }

    @Test(expected = AssertionFailedException.class)
    public void testMissingOneOfTwoRequirements() throws Exception {
        TestRequireClass p = new TestRequireClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"i\":1, \"b\":true}");
        this.mapfishJsonParser.parse(true, json, p);
    }

    @Test
    public void testFinalPublic() throws Exception {
        TestFinalClass p = new TestFinalClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{}");
        this.mapfishJsonParser.parse(true, json, p);
        assertEquals(100, p.i.intValue());

    }

    @Test(expected = ExtraPropertyException.class)
    public void testFinalFieldMustNotBeInJson() throws Exception {
        TestFinalClass p = new TestFinalClass();
        PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"i\":1}");
        this.mapfishJsonParser.parse(true, json, p);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testEnumIllegalVal() throws Exception {
        TestEnumParam p = new TestEnumParam();

        final PJsonObject json = AbstractMapfishSpringTest.parseJSONObjectFromString("{\"e1\":\"foo\", \"e2\": [2, \"VAL2\"]}");

        this.mapfishJsonParser.parse(true, json, p, "toIgnore");
    }

    @Test
    public void testPopulateLayerParam() throws Exception {
        final TestParam param = new TestParam();
        final PJsonObject json = parseJSONObjectFromFile(MapfishParserTest.class, "mapAttributeTest.json");

        this.mapfishJsonParser.parse(true, json, param, "toIgnore");

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
            this.mapfishJsonParser.parse(true, new PJsonObject(new JSONObject(), "spec"), param);
            fail("Expected an exception to be raised");
        } catch (MissingPropertyException e) {
            final Matcher missingParameterMatcher = Pattern.compile("\\*.+?").matcher(e.getMessage());
            int errorCount = 0;
            while (missingParameterMatcher.find()) {
                errorCount ++;
            }
            int totalAttributes = ParserUtils.getAllAttributeNames(param.getClass()).size();
            assertEquals(18 + totalAttributes, errorCount);

            assertEquals(18, e.getMissingProperties().size());
            assertEquals(totalAttributes, e.getAttributeNames().size());
        }

    }
    @Test
    public void testPopulateLayerParam_TooManyParams() throws Exception {
        final TestParam param = new TestParam();
        final PJsonObject json = parseJSONObjectFromFile(MapfishParserTest.class, "mapAttributeTest.json");
        json.getInternalObj().put("extraProperty", "value");
        json.getInternalObj().put("extraProperty2", "value2");
        try {
            this.mapfishJsonParser.parse(true, json, param, "toIgnore");
        } catch (ExtraPropertyException e) {
            final Matcher missingParameterMatcher = Pattern.compile("\\*.+?").matcher(e.getMessage());
            int errorCount = 0;
            while (missingParameterMatcher.find()) {
                errorCount ++;
            }
            int totalAttributes = ParserUtils.getAllAttributeNames(param.getClass()).size();
            assertEquals(2 + totalAttributes, errorCount);

            assertEquals(2, e.getExtraProperties().size());
            assertEquals(totalAttributes, e.getAttributeNames().size());
            assertTrue(e.getExtraProperties().contains("extraProperty"));
        }
    }

    @Test(expected = ExtraPropertyException.class)
    public void testDoesntMap() throws Exception {
        String legendAtts = "{\n"
                     + "    \"extra\": \"\",\n"
                     + "    \"classes\": [{\n"
                     + "        \"name\": \"osm\",\n"
                     + "        \"icons\": [\"http://localhost:9876/e2egeoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0"
                     + ".0&FORMAT=image/png&WIDTH=20&HEIGHT=20&LAYER=topp:states\"]\n"
                     + "    }]\n"
                     + "}\n";
        PObject requestData = new PJsonObject(new JSONObject(legendAtts), "legend");
        LegendAttribute.LegendAttributeValue param = new LegendAttribute.LegendAttributeValue();
        new MapfishParser().parse(true, requestData, param);

    }

    public static void populateLayerParam(PObject requestData, Object param, String... extraNamesToIgnore)
            throws IOException, JSONException {
        new MapfishParser().parse(true, requestData, param, extraNamesToIgnore);
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
        public PObject po;
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

    static class TestChoiceClass {
        @OneOf("choiceGroup1")
        public int choiceA;
        @OneOf("choiceGroup1")
        public double choiceB;
        @CanSatisfyOneOf("choiceGroup1")
        public int choiceC;
    }

    static class TestRequireClass {
        @HasDefaultValue
        @Requires({"b", "c"})
        public Integer i;
        @HasDefaultValue
        public Boolean b;
        @HasDefaultValue
        public Boolean c;
    }
    static class TestFinalClass {
        public final Integer i = 100;
    }
}
