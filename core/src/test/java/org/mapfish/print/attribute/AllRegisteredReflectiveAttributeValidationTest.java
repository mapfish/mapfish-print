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

import org.json.JSONWriter;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.StringWriter;
import java.util.List;

public class AllRegisteredReflectiveAttributeValidationTest extends AbstractMapfishSpringTest {
    @Autowired
    List<ReflectiveAttribute<?>> allAttribute;

    @Test
    public void testAllAttributesHaveLegalValues() throws Exception {
        for (ReflectiveAttribute<?> attribute : allAttribute) {
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
        for (ReflectiveAttribute<?> attribute : allAttribute) {
            attribute.init();
            attribute.setDefaultsForTesting();
            JSONWriter json = new JSONWriter(new StringWriter());
            json.object();
            attribute.printClientConfig(json, template);
            json.endObject();
        }

        // no exception... good
    }
}