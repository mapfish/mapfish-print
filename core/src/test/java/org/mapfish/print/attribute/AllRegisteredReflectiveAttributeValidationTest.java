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

package org.mapfish.print.attribute;

import com.google.common.collect.Maps;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.test.util.AttributeTesting;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.StringWriter;
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
    public void testAllAttributesHaveLegalValues() throws Exception {
        for (ReflectiveAttribute<?> attribute : allReflectiveAttributes) {
            attribute.init();
        }

        // no exception... good
    }
    @Test
    public void testAllPrintClientConfig() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setConfigurationFile(getFile("map/map_attributes/config-yaml.yaml"));
        Template template = new Template();
        template.setConfiguration(configuration);
        for (Attribute attribute : allAttributes) {
            final String attName = "!" + attribute.getClass().getSimpleName();

            Map<String, Attribute> attMap = Maps.newHashMap();
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