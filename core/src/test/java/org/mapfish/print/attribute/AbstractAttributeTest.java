package org.mapfish.print.attribute;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mockito.Mockito;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_ATTRIBUTE_TYPE;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_NAME;

/**
 * Common base class for testing attributes.
 */
public abstract class AbstractAttributeTest {

    public static JSONObject getClientConfig(Attribute attribute, Template template) throws JSONException {
        final StringWriter jsonOutput = new StringWriter();
        JSONWriter json = new JSONWriter(jsonOutput);
        json.object();
        attribute.printClientConfig(json, template);
        json.endObject();
        return new JSONObject(jsonOutput.toString());
    }

    @Test
    public void testPrintClientConfig() {
        final Attribute attribute = createAttribute();
        Template template = Mockito.mock(Template.class);
        JSONObject capabilities = getClientConfig(attribute, template);
        assertTrue("Missing " + JSON_NAME + " in: \n" + capabilities.toString(2),
                   capabilities.has(JSON_NAME));
        assertTrue("Missing " + JSON_ATTRIBUTE_TYPE + " in: \n" + capabilities.toString(2),
                   capabilities.has(JSON_ATTRIBUTE_TYPE));
    }

    @Test
    public void testValidate() {
        List<Throwable> errors = new ArrayList<>();
        Configuration configuration = new Configuration();
        createAttribute().validate(errors, configuration);

        assertTrue(errors.toString(), errors.isEmpty());
    }

    protected abstract Attribute createAttribute();
}
