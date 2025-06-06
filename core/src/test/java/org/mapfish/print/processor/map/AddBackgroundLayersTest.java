package org.mapfish.print.processor.map;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.batik.transcoder.TranscoderException;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Basic test of the background-layer processor.
 *
 * <p>Created by Jesse on 3/26/14.
 */
public class AddBackgroundLayersTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "add-background-layers/";

  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory requestFactory;
  @Autowired private ForkJoinPool forkJoinPool;

  private static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(AddBackgroundLayersTest.class, BASE_DIR + "requestData.json");
  }

  @Test
  @DirtiesContext
  public void testExecute() throws Exception {
    final String host = "center_wms1_0_0_flexiblescale";
    requestFactory.registerHandler(
        input ->
            (("" + input.getHost()).contains(host + ".wms"))
                || input.getAuthority().contains(host + ".wms"),
        createFileHandler("/map-data/tiger-ny.png"));
    requestFactory.registerHandler(
        input ->
            (("" + input.getHost()).contains(host + ".json"))
                || input.getAuthority().contains(host + ".json"),
        createFileHandler(uri -> "/map-data" + uri.getPath()));
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    final Template template = config.getTemplate("main");
    PJsonObject requestData = loadJsonRequestData();
    Values values =
        new Values(
            new HashMap<String, String>(),
            requestData,
            template,
            getTaskDirectory(),
            this.requestFactory,
            new File("."),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS,
            new AtomicBoolean(false));

    final ForkJoinTask<Values> taskFuture =
        this.forkJoinPool.submit(template.getProcessorGraph().createTask(values));
    taskFuture.get();

    assertImage(values, 1, "layerGraphics", "expectedSimpleImage.png", 630, 294, 0);
    assertImage(values, 1, "overviewMapLayerGraphics", "expectedOverviewImage.png", 320, 200, 0);
  }

  private void assertImage(
      Values values,
      int numberOfLayers,
      String graphicsValueKey,
      String imageName,
      int width,
      int height,
      double tolerance)
      throws IOException, TranscoderException {
    @SuppressWarnings("unchecked")
    List<URI> layerGraphics = (List<URI>) values.getObject(graphicsValueKey, List.class);
    assertEquals(numberOfLayers, layerGraphics.size());

    new ImageSimilarity(getFile(BASE_DIR + imageName))
        .assertSimilarity(layerGraphics, width, height, tolerance);
  }
}
