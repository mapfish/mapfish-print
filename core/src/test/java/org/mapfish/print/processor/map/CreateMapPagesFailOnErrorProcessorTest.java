package org.mapfish.print.processor.map;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Check that when the layer failOnError parameter is activated and a fetching error occurs, all the
 * tasks associated with the current printing processor are stopped quickly since the printing job
 * will fail anyway. We use the number of WMS calls during print generation to assess if the print
 * generation was stop quickly
 */
public class CreateMapPagesFailOnErrorProcessorTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "paging_processor_fail_on_error_test/";
  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory requestFactory;
  @Autowired private Map<String, OutputFormat> outputFormat;

  private static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        CreateMapPagesFailOnErrorProcessorTest.class, BASE_DIR + "requestData.json");
  }

  private static final AtomicInteger wmsRequestsNb = new AtomicInteger(0);

  @Before
  public void setUp() {
    final String host = "paging_processor_fail_on_error_test";
    requestFactory.registerHandler(
        input ->
            (("" + input.getHost()).contains(host + ".wms"))
                || (input.getAuthority() != null && input.getAuthority().contains(host + ".wms")),
        createFailingFileHandler(uri -> "/map-data/tiger-ny.png"));
  }

  /** File handler that will fail at the 40th request */
  protected TestHttpClientFactory.Handler createFailingFileHandler(Function<URI, String> filename) {
    return new TestHttpClientFactory.Handler() {
      @Override
      public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
        if (wmsRequestsNb.incrementAndGet() == 40) {
          // 404 will create a print error since we are using the WMS layer.
          // This will not happen on tiled layers.
          return error404(uri, httpMethod);
        }
        byte[] bytes = getFileBytes(filename.apply(uri));
        return ok(uri, bytes, httpMethod);
      }
    };
  }

  @Test
  @DirtiesContext
  public void testExecute() throws Exception {
    Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    PJsonObject requestData = loadJsonRequestData();
    final AbstractJasperReportOutputFormat format =
        (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");

    // Normal execution should make more around 400/500 WMS calls, but since fail on error is
    // activated and the 40Th call will fail,
    // less than 200 WMS calls should be done.
    testPrint(config, requestData, format, 200);
  }

  private void testPrint(
      Configuration config,
      PJsonObject requestData,
      AbstractJasperReportOutputFormat format,
      int maxWmsRequestsNumber) {
    try {
      format.getJasperPrint(
          new HashMap<>(), requestData, config, config.getDirectory(), getTaskDirectory());
      Assert.fail("Generation was not canceled");
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ExecutionException);
    } finally {
      Assert.assertTrue(
          String.format(
              "Number of WMS requests (%d) during print generation should be less than %d",
              wmsRequestsNb.get(), maxWmsRequestsNumber),
          wmsRequestsNb.get() < maxWmsRequestsNumber);
    }
  }
}
