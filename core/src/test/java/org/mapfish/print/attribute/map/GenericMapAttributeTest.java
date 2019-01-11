package org.mapfish.print.attribute.map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Test;
import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.test.util.AttributeTesting;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_ATTRIBUTE_DEFAULT;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_ATTRIBUTE_EMBEDDED_TYPE;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_ATTRIBUTE_IS_ARRAY;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_ATTRIBUTE_TYPE;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_CLIENT_INFO;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_CLIENT_PARAMS;

/**
 * Test reflective attribute functionality.
 */
public class GenericMapAttributeTest {

    @Test
    public void testPrintClientConfig() {
        final String mapAttName = "mainMap";

        final TestMapAttribute att = new TestMapAttribute();
        att.setConfigName(mapAttName);
        att.setDpiSuggestions(new double[]{72, 92.2, 128, 200.5});
        att.setMaxDpi(300.0);
        att.setWidth(128);
        att.setHeight(60);
        att.setZoomLevels(new ZoomLevels(1000, 2000, 3000, 4000));

        StringWriter stringWriter = new StringWriter();
        JSONWriter jsonWriter = new JSONWriter(stringWriter);
        jsonWriter.object();
        att.printClientConfig(jsonWriter, new Template());
        jsonWriter.endObject();
        JSONObject json = new JSONObject(stringWriter.toString());

        assertEquals(mapAttName, json.getString(ReflectiveAttribute.JSON_NAME));
        assertTrue(json.has(JSON_CLIENT_PARAMS));
        assertTrue(json.toString(2), json.has(JSON_CLIENT_INFO));

        final JSONObject required = json.getJSONObject(JSON_CLIENT_PARAMS);
        assertEquals(required.toString(2), 15, required.length());

        assertElem(required, "requiredElem", "int", null, false);
        assertElem(required, "pArray", "array", null, false);
        assertElem(required, "pObject", "object", null, false);
        assertElem(required, "requiredArray", "int", null, true);
        assertElem(required, "optionalArray", "int", "[1,2]", true);
        assertElem(required, "projection", "String", "null", false);
        assertElem(required, "rotation", "double", "null", false);
        assertElem(required, "useNearestScale", "boolean", "null", false);
        assertElem(required, "useAdjustBounds", "boolean", "null", false);
        assertElem(required, "longitudeFirst", "boolean", "null", false);
        assertElem(required, "dpiSensitiveStyle", "boolean", "true", false);
        assertEmbedded(required, false, "embedded");
        assertEmbedded(required, true, "optionalEmbedded");


        final JSONObject suggestions = json.getJSONObject(JSON_CLIENT_INFO);
        assertEquals(5, suggestions.length());

        assertEquals("[72,92.2,128,200.5]",
                     suggestions.get(GenericMapAttribute.JSON_DPI_SUGGESTIONS).toString()
                             .replaceAll("\\s+", ""));
        assertEquals("[4000,3000,2000,1000]",
                     suggestions.get(GenericMapAttribute.JSON_ZOOM_LEVEL_SUGGESTIONS).toString()
                             .replaceAll("\\s+", ""));
        assertEquals(300, suggestions.getInt(GenericMapAttribute.JSON_MAX_DPI));
        assertEquals(128, suggestions.getInt(GenericMapAttribute.JSON_MAP_WIDTH));
        assertEquals(60, suggestions.getInt(GenericMapAttribute.JSON_MAP_HEIGHT));
    }

    @Test
    public void testPrintClientConfigWithDefaults() {
        final TestMapAttribute att = new TestMapAttribute();
        AttributeTesting.configureAttributeForTesting(att);

        Map<String, Object> defaultValue = new HashMap<>();
        defaultValue.put("rotation", 1.0);
        HashMap<Object, Object> embeddedDefaultVal = new HashMap<>();
        embeddedDefaultVal.put("embeddedElem", true);
        defaultValue.put("embedded", embeddedDefaultVal);
        att.setDefault(defaultValue);

        StringWriter stringWriter = new StringWriter();
        JSONWriter jsonWriter = new JSONWriter(stringWriter);
        jsonWriter.object();
        att.printClientConfig(jsonWriter, new Template());
        jsonWriter.endObject();
        JSONObject json = new JSONObject(stringWriter.toString());

        final JSONObject required = json.getJSONObject(JSON_CLIENT_PARAMS);
        assertEquals(15, required.length());

        assertElem(required, "rotation", "double", "1", false);
        JSONObject embeddedDefault = required.getJSONObject("embedded").getJSONObject(JSON_ATTRIBUTE_DEFAULT);
        assertTrue(embeddedDefault.has("embeddedElem"));
        assertTrue(embeddedDefault.getBoolean("embeddedElem"));
    }

    private void assertElem(
            JSONObject required, String elemName, String type, String defaultVal, boolean isArray)
            throws JSONException {
        final JSONObject elem = required.getJSONObject(elemName);
        assertEquals(type, elem.getString(JSON_ATTRIBUTE_TYPE));
        if (defaultVal != null) {
            assertEquals(required.toString(2), defaultVal, elem.get(JSON_ATTRIBUTE_DEFAULT).toString());
        } else {
            assertFalse(required.toString(2), elem.has(JSON_ATTRIBUTE_DEFAULT));
        }
        if (isArray) {
            assertTrue(elem.getBoolean(JSON_ATTRIBUTE_IS_ARRAY));
        } else {
            assertFalse(elem.has(JSON_ATTRIBUTE_IS_ARRAY));
        }
    }

    private void assertEmbedded(JSONObject required, boolean hasDefault, String attName)
            throws JSONException {
        assertTrue(required.has(attName));
        final JSONObject embedded = required.getJSONObject(attName);
        assertEquals(hasDefault, embedded.has(JSON_ATTRIBUTE_DEFAULT));
        assertFalse(embedded.has(JSON_ATTRIBUTE_IS_ARRAY));
        if (hasDefault) {
            assertEquals("null", embedded.get(JSON_ATTRIBUTE_DEFAULT).toString());
        }
        assertEquals(MapfishParser.stringRepresentation(TestMapAttribute.EmbeddedTestAttribute.class),
                     embedded.get(JSON_ATTRIBUTE_TYPE));
        final JSONObject typeDescriptor = embedded.getJSONObject(JSON_ATTRIBUTE_EMBEDDED_TYPE);
        assertTrue(typeDescriptor.has("embeddedElem"));
        assertEquals(typeDescriptor.toString(2), 1, typeDescriptor.length());
        assertElem(typeDescriptor, "embeddedElem", "boolean", null, false);
    }
}
