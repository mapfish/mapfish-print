package org.mapfish.print.config;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

import static org.mapfish.print.AbstractMapfishSpringTest.DEFAULT_SPRING_XML;

/**
 * Test ConfigurationFactory.
 *
 */
public class ConfigurationFactoryAllConfigObjectMustBePrototypeTest {

    public static final String TEST_SPRING_XML =
            "classpath:org/mapfish/print/config/config-test-no-prototype-application-context.xml";

    @Test(expected = BeanCreationException.class)
    public void testAll() throws Exception {
        ClassPathXmlApplicationContext applicationContext =
                new ClassPathXmlApplicationContext(DEFAULT_SPRING_XML, TEST_SPRING_XML);
        ConfigurationFactory configurationFactory = applicationContext.getBean(ConfigurationFactory.class);
        File configFile = AbstractMapfishSpringTest
                .getFile(ConfigurationFactoryAllConfigObjectMustBePrototypeTest.class,
                         "configRequiringSpringInjection.yaml");
        configurationFactory.getConfig(configFile);
    }
}
