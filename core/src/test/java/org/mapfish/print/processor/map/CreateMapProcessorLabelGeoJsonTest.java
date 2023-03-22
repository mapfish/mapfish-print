package org.mapfish.print.processor.map;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import javax.imageio.ImageIO;
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

/** Test to check that longer labels that partially are outside the viewbox are rendered. */
public class CreateMapProcessorLabelGeoJsonTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "bbox_geojson_label_style/";

  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private MfClientHttpRequestFactoryImpl httpRequestFactory;
  @Autowired private ForkJoinPool forkJoinPool;

  public static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        CreateMapProcessorLabelGeoJsonTest.class, BASE_DIR + "requestData.json");
  }

  @Test
  public void testExecute() throws Exception {
    PJsonObject requestData = loadJsonRequestData();
    doTest(requestData);
  }

  private void doTest(PJsonObject requestData)
      throws IOException, JSONException, ExecutionException, InterruptedException {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
    final Template template = config.getTemplate("main");
    Values values =
        new Values(
            "test",
            requestData,
            template,
            getTaskDirectory(),
            this.httpRequestFactory,
            new File("."));

    final ForkJoinTask<Values> taskFuture =
        this.forkJoinPool.submit(template.getProcessorGraph().createTask(values));
    taskFuture.get();

    @SuppressWarnings("unchecked")
    List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
    assertEquals(1, layerGraphics.size());

    final BufferedImage img = ImageIO.read(new File(layerGraphics.get(0)));
    new ImageSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png")).assertSimilarity(img, 700);
  }
}
