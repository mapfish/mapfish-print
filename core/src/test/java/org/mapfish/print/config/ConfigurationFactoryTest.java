package org.mapfish.print.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.processor.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.yaml.snakeyaml.constructor.ConstructorException;

/** Test ConfigurationFactory. */
@ContextConfiguration(locations = {ConfigurationFactoryTest.TEST_SPRING_XML})
public class ConfigurationFactoryTest extends AbstractMapfishSpringTest {

  public static final String TEST_SPRING_XML =
      "classpath:org/mapfish/print/config/config-test-application-context.xml";
  @Autowired private ConfigurationFactory configurationFactory;

  @Before
  public void setUp() {
    this.configurationFactory.setDoValidation(false);
  }

  @Test
  public void testSpringInjection() throws Exception {
    File configFile =
        super.getFile(ConfigurationFactoryTest.class, "configRequiringSpringInjection.yaml");
    final Configuration config = configurationFactory.getConfig(configFile);

    assertNotNull(config.getDirectory());
    assertEquals(1, config.getTemplates().size());
    final Template template = config.getTemplate("main");
    assertNotNull(template);

    assertEquals(1, template.getAttributes().size());
    final Attribute attribute = template.getAttributes().get("att");
    assertTrue(attribute instanceof AttributeWithSpringInjection);
    ((AttributeWithSpringInjection) attribute).assertInjected();

    assertEquals(1, template.getProcessorGraph().getAllProcessors().size());
    assertEquals(1, template.getProcessorGraph().getRoots().size());
    Processor<?, ?> processor = template.getProcessorGraph().getRoots().getFirst().getProcessor();
    assertTrue(processor.toString(), processor instanceof ProcessorWithSpringInjection);
    ((ProcessorWithSpringInjection) processor).assertInjected();
  }

  @Test
  public void testParallelConfigLoading() throws Exception {
    File configFileThread1 = getFile(ConfigurationFactoryTest.class, "configThread1.yaml");
    File configFileThread2 = getFile(ConfigurationFactoryTest.class, "configThread2.yaml");
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    Callable<Boolean> task1 =
        () -> {
          startLatch.await();
          Configuration config = configurationFactory.getConfig(configFileThread1);
          if (config.getTemplates() != null && config.getTemplates().containsKey("main")) {
            return config.getTemplate("main").getAttributes().get("thread1") != null;
          }
          return false;
        };

    Callable<Boolean> task2 =
        () -> {
          startLatch.await();
          Configuration config = configurationFactory.getConfig(configFileThread2);
          if (config.getTemplates() != null && config.getTemplates().containsKey("main")) {
            return config.getTemplate("main").getAttributes().get("thread2") != null;
          }
          return false;
        };

    Future<Boolean> thread1Futur = executor.submit(task1);
    Future<Boolean> thread2Futur = executor.submit(task2);

    // Threads start at the same time
    startLatch.countDown();

    boolean thread1Result = thread1Futur.get();
    boolean thread2Result = thread2Futur.get();

    assertTrue("thread 1 configuration loading was not OK", thread1Result);
    assertTrue("thread 2 configuration loading was not OK", thread2Result);

    executor.shutdown();
  }

  @Test
  public void testConfigurationInjection() throws Exception {
    File configFile =
        super.getFile(ConfigurationFactoryTest.class, "configRequiringConfigurationInjection.yaml");
    final Configuration config = configurationFactory.getConfig(configFile);
    assertNotNull(config.getDirectory());

    assertEquals(1, config.getTemplates().size());
    final Template template = config.getTemplate("main");
    assertNotNull(template);

    assertEquals(1, template.getAttributes().size());
    final Attribute attribute = template.getAttributes().get("att");
    assertTrue(attribute instanceof AttributeWithConfigurationInjection);
    ((AttributeWithConfigurationInjection) attribute).assertInjected();

    assertEquals(1, template.getProcessorGraph().getAllProcessors().size());
    assertEquals(1, template.getProcessorGraph().getRoots().size());
    Processor processor = template.getProcessorGraph().getRoots().getFirst().getProcessor();
    assertTrue(processor instanceof ProcessorWithConfigurationInjection);
    ((ProcessorWithConfigurationInjection) processor).assertInjected();
  }

  @Test(expected = ConstructorException.class)
  public void testConfigurationAttributeMustImplementAttribute() throws Exception {
    File configFile =
        super.getFile(
            ConfigurationFactoryTest.class, "configWithProcessorAsAttribute_bad_config.yaml");
    configurationFactory.getConfig(configFile);
  }

  @Test(expected = ConstructorException.class)
  public void testConfigurationProcessorMustImplementProcessor() throws Exception {
    File configFile =
        super.getFile(
            ConfigurationFactoryTest.class, "configWithAttributeAsProcessor_bad_config.yaml");
    configurationFactory.getConfig(configFile);
  }
}
