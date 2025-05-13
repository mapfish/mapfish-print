package org.mapfish.print.processor.map;

import static org.junit.Assert.assertEquals;

import com.google.common.io.Files;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

/** CreateMapPages Processor test using zoomToFeature */
public class CreateMapPagesProcessorZoomToFeatureTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "paging_processor_zoom_to_features_test/";

  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory requestFactory;
  @Autowired private Map<String, OutputFormat> outputFormat;

  @Before
  public void setUp() throws Exception {
    final String host = "paging_processor_zoom_to_features_test";
    requestFactory.registerHandler(
        input ->
            (("" + input.getHost()).contains(host + ".json"))
                || input.getAuthority().contains(host + ".json"),
        new TestHttpClientFactory.Handler() {
          @Override
          public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod)
              throws Exception {
            try {
              byte[] bytes = Files.toByteArray(getFile("/map-data" + uri.getPath()));
              return ok(uri, bytes, httpMethod);
            } catch (AssertionError e) {
              return error404(uri, httpMethod);
            }
          }
        });
  }

  @Test
  @DirtiesContext
  public void testExecute() throws Exception {
    Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    PJsonObject requestData = loadJsonRequestData();

    final AbstractJasperReportOutputFormat format =
        (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
    testPrint(config, requestData, "zoom-to-features", format, 0);
  }

  private void testPrint(
      Configuration config,
      PJsonObject requestData,
      String testName,
      AbstractJasperReportOutputFormat format,
      double tolerance)
      throws Exception {
    JasperPrint print =
        format.getJasperPrint(
                new HashMap<>(), requestData, config, config.getDirectory(), getTaskDirectory())
            .print;
    assertEquals(7, print.getPages().size());
    for (int i = 0; i < print.getPages().size(); i++) {
      BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, i);
      new ImageSimilarity(
              getFile(String.format("%soutput/%s/expected-page-%s.png", BASE_DIR, testName, i)))
          .assertSimilarity(reportImage, tolerance);
    }
  }

  private static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        CreateMapPagesProcessorZoomToFeatureTest.class, BASE_DIR + "requestData.json");
  }
}
