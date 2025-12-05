package org.mapfish.print.processor.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import net.sf.jasperreports.engine.JasperPrint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @BeforeEach
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
    testPrint(config, requestData, "default-aoi", format, 0);

    getPagingAttributes(requestData).put("renderPagingOverview", "true");
    testPrint(config, requestData, "paging-overview-layer", format, 0);

    getPagingAttributes(requestData).put("pagingOverviewStyle", createPagingOverviewStyle());
    testPrint(config, requestData, "paging-overview-layer-styled", format, 0);
    getPagingAttributes(requestData).put("renderPagingOverview", "false");

    getAreaOfInterest(requestData).put("display", "CLIP");
    testPrint(config, requestData, "clip-full-aoi", format, 0);
    getAreaOfInterest(requestData).remove("display");

    requestData = loadJsonRequestData();

    getPagingAttributes(requestData).put("aoiDisplay", "clip");
    testPrint(config, requestData, "clip-page-aoi", format, 0);

    getPagingAttributes(requestData).put("aoiDisplay", "render");
    testPrint(config, requestData, "default-aoi", format, 0);

    getPagingAttributes(requestData).put("aoiDisplay", "none");
    testPrint(config, requestData, "none-aoi", format, 0);

    getAreaOfInterest(requestData).put("display", "CLIP");
    getPagingAttributes(requestData).put("aoiDisplay", "RENDER");
    testPrint(config, requestData, "full-clip-sub-render", format, 0);

    getAreaOfInterest(requestData).put("display", "CLIP");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    testPrint(config, requestData, "full-clip-sub-none", format, 0);

    getAreaOfInterest(requestData).put("display", "CLIP");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    testPrint(config, requestData, "full-clip-sub-none", format, 0);

    getAreaOfInterest(requestData).put("display", "NONE");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    testPrint(config, requestData, "all-none", format, 0);

    getAreaOfInterest(requestData).put("display", "NONE");
    getPagingAttributes(requestData).put("aoiDisplay", "NONE");
    getMapAttributes(requestData).put("dpi", 254);
    testPrint(config, requestData, "higher-dpi", format, 3);

    config = configurationFactory.getConfig(getFile(BASE_DIR + "config-geodetic.yaml"));
    requestData = loadJsonRequestData();
    testPrint(config, requestData, "geodetic", format, 0);

    getMapAttributes(requestData).put("dpi", 254);
    testPrint(config, requestData, "higher-dpi-geodetic", format, 3);

    config = configurationFactory.getConfig(getFile(BASE_DIR + "config-scalebar.yaml"));
    requestData = loadJsonRequestData();
    testPrint(config, requestData, "scalebar", format, 0);
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

  private JSONObject createPagingOverviewStyle() throws JSONException {
    JSONObject jsonStyle = new JSONObject();
    jsonStyle.put("version", "2");
    final JSONObject lineSymb = new JSONObject();
    lineSymb.put("type", "line");
    lineSymb.put("strokeColor", "blue");
    lineSymb.put("strokeDashstyle", "solid");
    lineSymb.put("strokeOpacity", "1");
    lineSymb.put("strokeWidth", "1.5");

    final JSONObject textSymb = new JSONObject();
    textSymb.put("type", "text");
    textSymb.put("label", "[name]");
    textSymb.put("haloRadius", "1.5");
    textSymb.put("haloColor", "yellow");
    textSymb.put("fontColor", "red");

    JSONArray symbs = new JSONArray();
    symbs.put(lineSymb);
    symbs.put(textSymb);

    JSONObject rule = new JSONObject();
    rule.put("symbolizers", symbs);
    jsonStyle.put("*", rule);
    return jsonStyle;
  }

  private void testPrint(
      Configuration config,
      PJsonObject requestData,
      String testName,
      AbstractJasperReportOutputFormat format,
      double tolerance)
      throws Exception {
    JasperPrint print =
        format
            .getJasperPrint(
                new HashMap<>(), requestData, config, config.getDirectory(), getTaskDirectory())
            .print();

    assertEquals(7, print.getPages().size());
    for (int i = 0; i < print.getPages().size(); i++) {
      BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, i);
      new ImageSimilarity(
              getFile(String.format("%soutput/%s/expected-page-%s.png", BASE_DIR, testName, i)))
          .assertSimilarity(reportImage, tolerance);
    }
  }
}
