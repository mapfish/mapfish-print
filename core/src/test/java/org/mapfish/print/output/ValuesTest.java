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

package org.mapfish.print.output;

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
import org.mapfish.print.parser.MapfishParser;
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
    private MapfishParser parser;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;

    @Test
    public void testNoDefaults() throws Exception {
        configurationFactory.setDoValidation(false);

        PJsonObject requestData = parseJSONObjectFromFile(ValuesTest.class, BASE_DIR + "requestData.json");

        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config-no-defaults.yaml"));

        Template template = config.getTemplates().values().iterator().next();
        final Values values = new Values(requestData, template, this.parser, new File("tmp"), this.httpRequestFactory, new File("."));

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
        new Values(requestData, template, this.parser, new File("tmp"), this.httpRequestFactory, new File("."));
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
        final Values values = new Values(requestData, template, this.parser, new File("tmp"), this.httpRequestFactory, new File("."));

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
}