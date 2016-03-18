package org.mapfish.print.attribute;

import com.google.common.collect.Lists;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mockito.Mockito;

import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_ATTRIBUTE_TYPE;
import static org.mapfish.print.attribute.ReflectiveAttribute.JSON_NAME;

/**
 * Common base class for testing attributes.
 *
 * @author Jesse on 5/8/2014.
 */
public abstract class AbstractAttributeTest {

    @Test
    public void testPrintClientConfig() throws Exception {
        final Attribute attribute = createAttribute();
        Template template = Mockito.mock(Template.class);
        JSONObject capabilities = getClientConfig(attribute, template);
        assertTrue("Missing " + JSON_NAME + " in: \n" + capabilities.toString(2), capabilities.has(JSON_NAME));
        assertTrue("Missing " + JSON_ATTRIBUTE_TYPE + " in: \n" + capabilities.toString(2), capabilities.has(JSON_ATTRIBUTE_TYPE));
    }

    public static JSONObject getClientConfig(Attribute attribute, Template template) throws JSONException {
        final StringWriter jsonOutput = new StringWriter();
        JSONWriter json = new JSONWriter(jsonOutput);
        json.object();
        attribute.printClientConfig(json, template);
        json.endObject();
        return new JSONObject(jsonOutput.toString());
    }

    @Test
    public void testValidate() throws Exception {
        List<Throwable> errors = Lists.newArrayList();
        Configuration configuration = new Configuration();
        createAttribute().validate(errors, configuration);

        assertTrue(errors.toString(), errors.isEmpty());
    }

    protected abstract Attribute createAttribute();
}
