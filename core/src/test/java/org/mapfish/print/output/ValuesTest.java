package org.mapfish.print.output;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.DataSourceAttribute;
import org.mapfish.print.attribute.LegendAttribute;
import org.mapfish.print.attribute.TableAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test creating values objects.
 */
public class ValuesTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "values/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;

    @Test
    public void testNoDefaults() throws Exception {
        configurationFactory.setDoValidation(false);

        PJsonObject requestData = parseJSONObjectFromFile(ValuesTest.class, BASE_DIR + "requestData.json");

        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-no-defaults.yaml"));

        Template template = config.getTemplates().values().iterator().next();
        final Values values = new Values("test", requestData, template, new File("tmp"), this.httpRequestFactory, new File("."));

        assertTrue(values.containsKey("title"));
        assertEquals("title", values.getString("title"));
        assertEquals(134, values.getInteger("count").intValue());
        assertEquals(2.0, values.getObject("ratio", Double.class), 0.00001);
        assertTrue(values.containsKey("legend"));
        assertEquals("legendName", values.getObject("legend", LegendAttribute.LegendAttributeValue.class).name);

        assertTrue(values.containsKey("datasource"));
        final Map<String, Object>[] attributesValues = values.getObject("datasource", DataSourceAttribute.DataSourceAttributeValue.class)
                .attributesValues;
        assertEquals(1, attributesValues.length);
        assertEquals("requestDataName", attributesValues[0].get("name"));
        final TableAttribute.TableAttributeValue table = (TableAttribute.TableAttributeValue) attributesValues[0].get("table");
        assertEquals("requestId", table.columns[0]);
    }

    @Test(expected = ObjectMissingException.class)
    public void testNoDefaults_Error_OnMissing_Attribute() throws Exception {
        configurationFactory.setDoValidation(false);
        final JSONObject obj = new JSONObject();
        PJsonObject requestData = new PJsonObject(obj, "");
        final JSONObject atts = new JSONObject();
        obj.put("attributes", atts);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-no-defaults.yaml"));

        Template template = config.getTemplates().values().iterator().next();
        new Values("test", requestData, template, new File("tmp"), this.httpRequestFactory, new File("."));
    }

    @Test
    public void testDefaults() throws Exception {
        configurationFactory.setDoValidation(false);
        final JSONObject obj = new JSONObject();
        PJsonObject requestData = new PJsonObject(obj, "");
        final JSONObject atts = new JSONObject();
        obj.put("attributes", atts);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-defaults.yaml"));

        Template template = config.getTemplates().values().iterator().next();
        final Values values = new Values("test", requestData, template, new File("tmp"), this.httpRequestFactory, new File("."));

        assertTrue(values.containsKey("title"));
        assertEquals("title", values.getString("title"));
        assertEquals(134, values.getInteger("count").intValue());
        assertEquals(2.0, values.getObject("ratio", Double.class), 0.00001);
        assertTrue(values.containsKey("legend"));
        assertEquals("legendName", values.getObject("legend", LegendAttribute.LegendAttributeValue.class).name);
        assertTrue(values.containsKey("datasource"));
        final Map<String, Object>[] attributesValues = values.getObject("datasource", DataSourceAttribute.DataSourceAttributeValue.class)
                .attributesValues;
        assertEquals(1, attributesValues.length);
        assertEquals("name", attributesValues[0].get("name"));
        final TableAttribute.TableAttributeValue table = (TableAttribute.TableAttributeValue) attributesValues[0].get("table");
        assertEquals("id", table.columns[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExpectObjectGetArray() throws Exception {

        PJsonObject requestData = parseJSONObjectFromFile(ValuesTest.class, BASE_DIR + "requestData.json");
        String badLegendConf = "[{\n"
                               + "    \"name\": \"\",\n"
                               + "    \"classes\": [{\n"
                               + "        \"name\": \"osm\",\n"
                               + "        \"icons\": [\"http://localhost:9876/e2egeoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0"
                               + ".0&FORMAT=image/png&WIDTH=20&HEIGHT=20&LAYER=topp:states\"]\n"
                               + "    }]\n"
                               + "}]\n";
        requestData.getInternalObj().getJSONObject("attributes").put("legend", new JSONArray(badLegendConf));
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-no-defaults.yaml"));

        Template template = config.getTemplates().values().iterator().next();
        new Values("test", requestData, template, new File("tmp"), this.httpRequestFactory, new File("."));
    }
}
