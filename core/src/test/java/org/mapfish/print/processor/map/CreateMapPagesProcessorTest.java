package org.mapfish.print.processor.map;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import net.sf.jasperreports.engine.JasperPrint;
import org.json.JSONException;
import org.json.JSONObject;
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
import org.springframework.test.annotation.DirtiesContext;

/**
 * Basic test of the Map processor.
 *
 * <p>Created by Jesse on 3/26/14.
 */
public class CreateMapPagesProcessorTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "paging_processor_test/";
  @Autowired ForkJoinPool forkJoinPool;
  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory requestFactory;
  @Autowired private Map<String, OutputFormat> outputFormat;

  private static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        CreateMapPagesProcessorTest.class, BASE_DIR + "requestData.json");
  }

  @Before
  public void setUp() {
    final String host = "paging_processor_test";
    requestFactory.registerHandler(
        input ->
            (("" + input.getHost()).contains(host + ".json"))
                || input.getAuthority().contains(host + ".json"),
        createFileHandler(uri -> "/map-data" + uri.getPath()));
  }

  @Test
  @DirtiesContext
  public void testExecute() throws Exception {
    Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    PJsonObject requestData = loadJsonRequestData();

    final AbstractJasperReportOutputFormat format =
        (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
    testPrint(config, requestData, "default-aoi", format, 40);

    getAreaOfInterest(requestData).put("display", "CLIP");
    testPrint(config, requestData, "clip-full-aoi", format, 30);
    getAreaOfInterest(requestData).remove("display");

    requestData = loadJsonRequestData();

    getPagingAttributes(requestData).put("aoiDisplay", "clip");
    testPrint(config, requestData, "clip-page-aoi", format, 40);

    getPagingAttributes(requestData).put("aoiDisplay", "render");
    testPrint(config, requestData, "default-aoi", format, 40);

    getPagingAttributes(requestData).put("aoiDisplay", "none");
    testPrint(config, requestData, "none-aoi", format, 40);

    getAreaOfInterest(requestData).put("display", "CLIP");
    getPagingAttributes(requestData).put("aoiDisplay", "RENDER");
    testPrint(config, requestData, "full-clip-sub-render", format, 40);

    getAreaOfInterest(requestData).put("display", "CLIP");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    testPrint(config, requestData, "full-clip-sub-none", format, 40);

    getAreaOfInterest(requestData).put("display", "CLIP");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    testPrint(config, requestData, "full-clip-sub-none", format, 40);

    getAreaOfInterest(requestData).put("display", "NONE");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    testPrint(config, requestData, "all-none", format, 40);

    getAreaOfInterest(requestData).put("display", "NONE");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    getMapAttributes(requestData).put("dpi", 254);
    testPrint(config, requestData, "higher-dpi", format, 40);

    config = configurationFactory.getConfig(getFile(BASE_DIR + "config-scalebar.yaml"));
    requestData = loadJsonRequestData();
    testPrint(config, requestData, "scalebar", format, 45);
  }

  private JSONObject getAreaOfInterest(PJsonObject requestData) throws JSONException {
    return getMapAttributes(requestData).getJSONObject("areaOfInterest");
  }

  private JSONObject getPagingAttributes(PJsonObject requestData) throws JSONException {
    final PJsonObject attributes = requestData.getJSONObject("attributes");
    JSONObject paging = attributes.getInternalObj().optJSONObject("paging");
    if (paging == null) {
      paging = new JSONObject();
      attributes.getInternalObj().put("paging", paging);
    }
    return paging;
  }

  private JSONObject getMapAttributes(PJsonObject requestData) throws JSONException {
    final PJsonObject attributes = requestData.getJSONObject("attributes");
    return attributes.getInternalObj().optJSONObject("map");
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
                "test", requestData, config, config.getDirectory(), getTaskDirectory())
            .print;

    assertEquals(7, print.getPages().size());
    for (int i = 0; i < print.getPages().size(); i++) {
      BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, i);
      new ImageSimilarity(
              getFile(String.format("%soutput/%s/expected-page-%s.png", BASE_DIR, testName, i)))
          .assertSimilarity(reportImage, tolerance);
    }
  }
}
