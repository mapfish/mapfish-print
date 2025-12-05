package org.mapfish.print.processor.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.URIUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/** Test for native rotation on WMS layers. */
public class CreateMapProcessorScaleBBoxNativeRotationWms1_3_0Test
    extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "bbox_native_rotation_wms1_3_0_scale/";

  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private TestHttpClientFactory requestFactory;

  @Autowired private ForkJoinPool forkJoinPool;

  private static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        CreateMapProcessorScaleBBoxNativeRotationWms1_3_0Test.class, BASE_DIR + "requestData.json");
  }

  @Test
  @DirtiesContext
  public void testExecute() throws Exception {
    final String host = "bbox_native_rotation_wms1_3_0_scale";
    requestFactory.registerHandler(
        input ->
            (("" + input.getHost()).contains(host + ".wms"))
                || input.getAuthority().contains(host + ".wms"),
        createFileHandler(
            uri -> {
              final Multimap<String, String> uppercaseParams = HashMultimap.create();
              for (Map.Entry<String, String> entry : URIUtils.getParameters(uri).entries()) {
                uppercaseParams.put(entry.getKey().toUpperCase(), entry.getValue().toUpperCase());
              }

              assertTrue(
                  uppercaseParams.containsEntry("SERVICE", "WMS"),
                  "SERVICE != WMS: " + uppercaseParams.get("WMS"));
              assertTrue(
                  uppercaseParams.containsEntry("FORMAT", "IMAGE/PNG"),
                  "FORMAT != IMAGE/PNG: " + uppercaseParams.get("FORMAT"));
              assertTrue(
                  uppercaseParams.containsEntry("REQUEST", "GETMAP"),
                  "REQUEST != GETMAP: " + uppercaseParams.get("REQUEST"));
              assertTrue(
                  uppercaseParams.containsEntry("VERSION", "1.3.0"),
                  "VERSION != 1.3.0: " + uppercaseParams.get("VERSION"));
              assertTrue(
                  uppercaseParams.containsEntry("LAYERS", "TOPP:STATES"),
                  "LAYERS != TOPP:STATES: " + uppercaseParams.get("LAYERS"));
              assertTrue(uppercaseParams.containsEntry("ANGLE", "90.0"), "ANGLE != 90");
              assertTrue(uppercaseParams.containsKey("BBOX"), "BBOX is missing");
              assertTrue(
                  uppercaseParams.containsEntry("WIDTH", "780"), "mapSize is not rotated (width)");
              assertTrue(
                  uppercaseParams.containsEntry("HEIGHT", "330"),
                  "mapSize is not rotated (height)");
              return "/map-data/states-native-rotation.png";
            }));
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    final Template template = config.getTemplate("main");
    PJsonObject requestData = loadJsonRequestData();
    Values values =
        new Values(
            new HashMap<>(),
            requestData,
            template,
            getTaskDirectory(),
            this.requestFactory,
            new File("."),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS,
            new AtomicBoolean(false));
    forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

    @SuppressWarnings("unchecked")
    List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
    assertEquals(1, layerGraphics.size());

    new ImageSimilarity(new File(layerGraphics.getFirst()))
        .assertSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"), 0);
  }
}
