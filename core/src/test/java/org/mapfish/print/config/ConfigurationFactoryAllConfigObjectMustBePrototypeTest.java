package org.mapfish.print.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mapfish.print.AbstractMapfishSpringTest.DEFAULT_SPRING_XML;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/** Test ConfigurationFactory. */
public class ConfigurationFactoryAllConfigObjectMustBePrototypeTest {

  public static final String TEST_SPRING_XML =
      "classpath:org/mapfish/print/config/config-test-no-prototype-application-context.xml";

  @Test
  public void testAll() throws Exception {
    assertThrows(BeanCreationException.class, () -> {
      ClassPathXmlApplicationContext applicationContext =
          new ClassPathXmlApplicationContext(DEFAULT_SPRING_XML, TEST_SPRING_XML);
      ConfigurationFactory configurationFactory =
          applicationContext.getBean(ConfigurationFactory.class);
      File configFile =
          AbstractMapfishSpringTest.getFile(
              ConfigurationFactoryAllConfigObjectMustBePrototypeTest.class,
              "configRequiringSpringInjection.yaml");
      configurationFactory.getConfig(configFile);
    });
  }
}
