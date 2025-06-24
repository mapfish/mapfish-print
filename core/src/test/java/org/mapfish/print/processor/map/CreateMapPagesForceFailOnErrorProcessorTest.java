package org.mapfish.print.processor.map;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Timeout;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.PrintException;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Test that verify that forceFailOnError apply the failOnError parameter on all requested layers
 */
public class CreateMapPagesForceFailOnErrorProcessorTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "paging_processor_force_fail_on_error_test/";
  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory requestFactory;
  @Autowired private Map<String, OutputFormat> outputFormat;

  private static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        CreateMapPagesForceFailOnErrorProcessorTest.class, BASE_DIR + "requestData.json");
  }

  private static final AtomicBoolean hasFailOnce = new AtomicBoolean(false);

  /** File handler that will fail on the first request. */
  protected TestHttpClientFactory.Handler createFailingFileHandler(Function<URI, String> filename) {
    return new TestHttpClientFactory.Handler() {
      @Override
      public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
        if (hasFailOnce.compareAndSet(false, true)) {
          MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
          MockClientHttpResponse response =
              new MockClientHttpResponse(new byte[0], HttpStatus.REQUEST_TIMEOUT);
          request.setResponse(response);
          return request;
        }
        byte[] bytes = getFileBytes(filename.apply(uri));
        return ok(uri, bytes, httpMethod);
      }
    };
  }

  @Test
  @DirtiesContext
  @Timeout(value = 1, unit = TimeUnit.MINUTES)
  public void testExecute() throws Exception {
    final String host = "paging_processor_force_fail_on_error_test";
    Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    PJsonObject requestData = loadJsonRequestData();
    final AbstractJasperReportOutputFormat format =
        (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");

    // .wms will fail once
    requestFactory.registerHandler(
        input -> (input.getAuthority() != null && input.getAuthority().contains(host + ".wms")),
        createFailingFileHandler(uri -> "/map-data/tiger-ny.png"));
    requestFactory.registerHandler(
        input -> (input.getAuthority() != null && input.getAuthority().contains(host + ".wmts")),
        createFileHandler(uri -> "/map-data/tiger-ny.png"));
    testPrint(config, requestData, format, true);
    config.setForceFailOnError(false);
    testPrint(config, requestData, format, false);

    // .wmts will fail once and create a PrintException error
    config.setForceFailOnError(true);
    requestFactory.resetHandlers();
    hasFailOnce.set(false);
    requestFactory.registerHandler(
        input -> (input.getAuthority() != null && input.getAuthority().contains(host + ".wms")),
        createFileHandler(uri -> "/map-data/tiger-ny.png"));
    requestFactory.registerHandler(
        input -> (input.getAuthority() != null && input.getAuthority().contains(host + ".wmts")),
        createFailingFileHandler(uri -> "/map-data/tiger-ny.png"));
    testPrint(config, requestData, format, true);
    config.setForceFailOnError(false);
    testPrint(config, requestData, format, false);
  }

  private void testPrint(
      Configuration config,
      PJsonObject requestData,
      AbstractJasperReportOutputFormat format,
      boolean shouldFail) {
    try {
      format.getJasperPrint(
          new HashMap<>(), requestData, config, config.getDirectory(), getTaskDirectory());
      if (shouldFail) {
        Assert.fail("Generation was not canceled");
      }
    } catch (Exception e) {
      if (!shouldFail) {
        Assert.fail("Generation was canceled");
      }
      // WMS interrupt cause is CancellationException or RuntimeException
      // WMTS interrupt cause is PrintException
      Assert.assertTrue(
          String.format(
              "Exception cause should be CancellationException or PrintException with message that"
                  + " contains 'Failed to compute Coverage Task' or RuntimeException with message"
                  + " that contains 'Request Timeout' but was %s, exception : %s",
              e.getCause(), e),
          e.getCause() instanceof CancellationException
              || (e.getCause() instanceof PrintException
                  && e.getMessage().contains("Failed to compute Coverage Task"))
              || (e.getCause() instanceof RuntimeException
                  && e.getMessage().contains("Request Timeout")));
    }
  }
}
