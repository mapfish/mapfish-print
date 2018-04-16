package org.mapfish.print.attribute;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataSourceAttributeTest extends AbstractMapfishSpringTest {

    @Autowired
    private ConfigurationFactory configurationFactory;

    @Test
    public void testParseRequest() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile("datasource/config.yaml"));
        final Template template = config.getTemplates().values().iterator().next();

        final Map<String, Attribute> attributes = template.getAttributes();

        PJsonObject jsonData = parseJSONObjectFromFile(DataSourceAttributeTest.class, "datasource/requestData.json");
        final Values values = new Values();
        values.populateFromAttributes(template, attributes, jsonData);

        final Class<DataSourceAttribute.DataSourceAttributeValue> type = DataSourceAttribute.DataSourceAttributeValue.class;
        final DataSourceAttribute.DataSourceAttributeValue value = values.getObject("datasource", type);
        assertNotNull(value);

        assertEquals(2, value.attributesValues.length);
        Map<String, Object> val1 = value.attributesValues[0];
        Map<String, Object> val2 = value.attributesValues[1];

        assertEquals(3, val1.size());
        assertEquals(3, val2.size());

        assertEquals("name1", val1.get("name"));
        assertEquals("name2", val2.get("name"));
        assertEquals(1, val1.get("count"));
        assertEquals(2, val2.get("count"));

        assertTrue(val1.get("table") instanceof TableAttribute.TableAttributeValue);
        assertTrue(val2.get("table") instanceof TableAttribute.TableAttributeValue);

        TableAttribute.TableAttributeValue table1 = (TableAttribute.TableAttributeValue) val1.get("table");
        TableAttribute.TableAttributeValue table2 = (TableAttribute.TableAttributeValue) val2.get("table");

        assertEquals(3, table1.columns.length);
        assertEquals(4, table2.columns.length);

        assertEquals(2, table1.data.length);
        assertEquals(1, table2.data.length);

        assertEquals(3, table1.data[0].size());
        assertEquals(3, table1.data[1].size());
        assertEquals(4, table2.data[0].size());
    }
}
