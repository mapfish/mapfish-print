package org.mapfish.print.processor.map;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONException;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.output.Values;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

/** Tests a GeoJSON layer with an empty FeatureCollection. */
public class CreateMapProcessorCenterGeojsonEmptyCollection extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "center_geojson_empty_collection/";

  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private MfClientHttpRequestFactoryImpl httpRequestFactory;
  @Autowired private ForkJoinPool forkJoinPool;

  public static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        CreateMapProcessorCenterGeojsonEmptyCollection.class, BASE_DIR + "requestData.json");
  }

  @Test
  public void testExecute() throws Exception {
    PJsonObject requestData = loadJsonRequestData();
    doTest(requestData);
  }

  private void doTest(final PJsonObject requestData)
      throws IOException, JSONException, ExecutionException, InterruptedException {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    final Template template = config.getTemplate("main");
    Values values =
        new Values(
            new HashMap<String, String>(),
            requestData,
            template,
            getTaskDirectory(),
            this.httpRequestFactory,
            new File("."),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS,
            new AtomicBoolean(false));

    final ForkJoinTask<Values> taskFuture =
        this.forkJoinPool.submit(template.getProcessorGraph().createTask(values));
    taskFuture.get();

    @SuppressWarnings("unchecked")
    List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
    assertEquals(1, layerGraphics.size());

    new ImageSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"))
        .assertSimilarity(new File(layerGraphics.get(0)), 0);
  }
}
