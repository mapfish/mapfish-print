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

package org.mapfish.print.config;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.processor.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test ConfigurationFactory.
 * <p/>
 * Created by Jesse on 3/25/14.
 */

@ContextConfiguration(locations = { ConfigurationFactoryTest.TEST_SPRING_XML}, inheritLocations = true)
public class ConfigurationFactoryTest extends AbstractMapfishSpringTest {

    public static final String TEST_SPRING_XML = "classpath:org/mapfish/print/config/config-test-application-context.xml";
    @Autowired
    private ConfigurationFactory configurationFactory;

    @Test
    public void testSpringInjection() throws Exception {
        File configFile = super.getFile(ConfigurationFactoryTest.class, "configRequiringSpringInjection.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);

        assertNotNull(config.getDirectory());
        assertEquals(1, config.getTemplates().size());
        final Template template = config.getTemplate("main");
        assertNotNull(template);

        assertEquals(1, template.getAttributes().size());
        final Attribute<?> attribute = template.getAttributes().get("att");
        assertTrue(attribute instanceof AttributeWithSpringInjection);
        ((AttributeWithSpringInjection)attribute).assertInjected();

        assertEquals(1, template.getProcessorGraph().getAllProcessors().size());
        assertEquals(1, template.getProcessorGraph().getRoots().size());
        Processor processor = template.getProcessorGraph().getRoots().get(0).getProcessor();
        assertTrue(processor.toString(), processor instanceof ProcessorWithSpringInjection);
        ((ProcessorWithSpringInjection)processor).assertInjected();
    }
    @Test
    public void testConfigurationInjection() throws Exception {
        File configFile = super.getFile(ConfigurationFactoryTest.class, "configRequiringConfigurationInjection.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);
        assertNotNull(config.getDirectory());

        assertEquals(1, config.getTemplates().size());
        final Template template = config.getTemplate("main");
        assertNotNull(template);

        assertEquals(1, template.getAttributes().size());
        final Attribute<?> attribute = template.getAttributes().get("att");
        assertTrue(attribute instanceof AttributeWithConfigurationInjection);
        ((AttributeWithConfigurationInjection)attribute).assertInjected();

        assertEquals(1, template.getProcessorGraph().getAllProcessors().size());
        assertEquals(1, template.getProcessorGraph().getRoots().size());
        Processor processor = template.getProcessorGraph().getRoots().get(0).getProcessor();
        assertTrue(processor instanceof ProcessorWithConfigurationInjection);
        ((ProcessorWithConfigurationInjection)processor).assertInjected();
    }

    @Test(expected = ConstructorException.class)
    public void testConfigurationAttributeMustImplementAttribute() throws Exception {
        File configFile = super.getFile(ConfigurationFactoryTest.class, "configWithProcessorAsAttribute_bad_config.yaml");
        configurationFactory.getConfig(configFile);
    }

    @Test(expected = ConstructorException.class)
    public void testConfigurationProcessorMustImplementProcessor () throws Exception {
        File configFile = super.getFile(ConfigurationFactoryTest.class, "configWithAttributeAsProcessor_bad_config.yaml");
        configurationFactory.getConfig(configFile);
    }
}
