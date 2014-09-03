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
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

import static org.mapfish.print.AbstractMapfishSpringTest.DEFAULT_SPRING_XML;

/**
 * Test ConfigurationFactory.
 * <p/>
 * @author jesseeichar on 3/25/14.
 */
public class ConfigurationFactoryAllConfigObjectMustBePrototypeTest {

    public static final String TEST_SPRING_XML = "classpath:org/mapfish/print/config/config-test-no-prototype-application-context.xml";

    @Test(expected = BeanCreationException.class)
    public void testAll() throws Exception {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(DEFAULT_SPRING_XML, TEST_SPRING_XML);
        ConfigurationFactory configurationFactory = applicationContext.getBean(ConfigurationFactory.class);
        File configFile = AbstractMapfishSpringTest.getFile(ConfigurationFactoryAllConfigObjectMustBePrototypeTest.class,
                "configRequiringSpringInjection.yaml");
        configurationFactory.getConfig(configFile);
    }
}
