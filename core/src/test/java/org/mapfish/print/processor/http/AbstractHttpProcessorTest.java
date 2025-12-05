package org.mapfish.print.processor.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mapfish.print.output.Values.MDC_CONTEXT_KEY;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nullable;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.*;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

public abstract class AbstractHttpProcessorTest extends AbstractMapfishSpringTest {
  @Autowired ConfigurationFactory configurationFactory;
  @Autowired TestHttpClientFactory httpClientFactory;

  @Autowired ForkJoinPool forkJoinPool;

  protected abstract String baseDir();

  protected abstract Class<? extends AbstractTestProcessor> testProcessorClass();

  protected abstract Class<? extends HttpProcessor> classUnderTest();

  @Test
  @DirtiesContext
  public void testExecute() throws Exception {
    this.httpClientFactory.registerHandler(
        input -> true,
        new TestHttpClientFactory.Handler() {
          @Override
          public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) {
            return new MockClientHttpRequest(httpMethod, uri);
          }
        });

    this.configurationFactory.setDoValidation(false);
    final Configuration config =
        configurationFactory.getConfig(getFile(baseDir() + "/config.yaml"));
    final Template template = config.getTemplate("main");

    ProcessorDependencyGraph graph = template.getProcessorGraph();
    List<ProcessorGraphNode<?, ?>> roots = graph.getRoots();

    assertEquals(1, roots.size());
    final ProcessorGraphNode<?, ?> processor = roots.getFirst();
    assertEquals(classUnderTest(), processor.getProcessor().getClass());

    final Set<? extends Processor<?, ?>> dependencies = processor.getAllProcessors();
    dependencies.remove(processor.getProcessor());
    assertEquals(1, dependencies.size());
    assertEquals(testProcessorClass(), dependencies.iterator().next().getClass());

    Values values = new Values();
    values.put(
        Values.CLIENT_HTTP_REQUEST_FACTORY_KEY,
        new MfClientHttpRequestFactoryProvider(this.httpClientFactory));
    values.put(Values.VALUES_KEY, values);
    values.put(MDC_CONTEXT_KEY, new HashMap<>());
    addExtraValues(values);
    forkJoinPool.invoke(graph.createTask(values));
  }

  protected void addExtraValues(Values values) throws JSONException {
    // default does nothing
  }

  @Test
  @DirtiesContext
  public void testCreateMapDependency() throws Exception {

    this.configurationFactory.setDoValidation(false);
    final Configuration config =
        configurationFactory.getConfig(getFile(baseDir() + "/config-createmap.yaml"));
    final Template template = config.getTemplate("main");

    ProcessorDependencyGraph graph = template.getProcessorGraph();
    List<ProcessorGraphNode<?, ?>> roots = graph.getRoots();

    assertEquals(1, roots.size());
    final ProcessorGraphNode<?, ?> compositeClientHttpRequestFactoryProcessor = roots.getFirst();
    assertEquals(
        classUnderTest(), compositeClientHttpRequestFactoryProcessor.getProcessor().getClass());
    final Set<? extends Processor<?, ?>> dependencies =
        compositeClientHttpRequestFactoryProcessor.getAllProcessors();
    dependencies.remove(compositeClientHttpRequestFactoryProcessor.getProcessor());
    assertEquals(1, dependencies.size());
    assertEquals(CreateMapProcessor.class, dependencies.iterator().next().getClass());
  }

  public static class TestParam {
    @InputOutputValue public MfClientHttpRequestFactoryProvider clientHttpRequestFactoryProvider;
  }

  public abstract static class AbstractTestProcessor extends AbstractProcessor<TestParam, Void> {

    /** Constructor. */
    protected AbstractTestProcessor() {
      super(Void.class);
    }

    @Override
    protected void extraValidation(
        List<Throwable> validationErrors, final Configuration configuration) {
      // do nothing
    }

    @Nullable
    @Override
    public TestParam createInputParameter() {
      return new TestParam();
    }
  }
}
