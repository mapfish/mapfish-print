package org.mapfish.print.attribute;

import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.test.util.AttributeTesting;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AllRegisteredReflectiveAttributeValidationTest extends AbstractMapfishSpringTest {
    @Autowired
    List<ReflectiveAttribute<?>> allReflectiveAttributes;

    @Autowired
    List<Attribute> allAttributes;

    @Test
    public void testAllAttributesHaveLegalValues() {
        for (ReflectiveAttribute<?> attribute: allReflectiveAttributes) {
            attribute.init();
        }

        // no exception... good
    }

    @Test
    public void testAllPrintClientConfig() {
        Configuration configuration = new Configuration();
        configuration.setConfigurationFile(getFile("map/map_attributes/config-yaml.yaml"));
        Template template = new Template();
        template.setConfiguration(configuration);
        for (Attribute attribute: allAttributes) {
            final String attName = "!" + attribute.getClass().getSimpleName();

            Map<String, Attribute> attMap = new HashMap<>();
            attMap.put(attName, attribute);
            template.setAttributes(attMap);
            if (attribute instanceof ReflectiveAttribute<?>) {
                ReflectiveAttribute<?> reflectiveAttribute = (ReflectiveAttribute<?>) attribute;

                AttributeTesting.configureAttributeForTesting(reflectiveAttribute);
            }

            final StringWriter w = new StringWriter();
            JSONWriter json = new JSONWriter(w);
            json.object();
            attribute.printClientConfig(json, template);
            json.endObject();

            final JSONObject config = new JSONObject(w.toString());
            assertNotNull(config.getString("name"));
            assertEquals(attName, config.getString("name"));
        }

        // no exception... good
    }
}
