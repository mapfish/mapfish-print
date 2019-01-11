package org.mapfish.print.processor.map;

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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import static org.junit.Assert.assertEquals;

/**
 * Test for the CreateOverviewMap processor, where the main map is rotated by 90 degree, but the overview map
 * is not rotated.
 */
public class CreateOverviewMapProcessorRotateTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "overview_map_rotate/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateOverviewMapProcessorRotateTest.class,
                                       BASE_DIR + "requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "overview_map_rotate";
        requestFactory.registerHandler(
                input -> (("" + input.getHost()).contains(host + ".osm")) ||
                        input.getAuthority().contains(host + ".osm"),
                createFileHandler(uri -> "/map-data/osm" + uri.getPath())
        );
        requestFactory.registerHandler(
                input -> (("" + input.getHost()).contains(host + ".json")) ||
                        input.getAuthority().contains(host + ".json"),
                createFileHandler(uri -> "/map-data" + uri.getPath())
        );
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(),
                                   this.requestFactory, new File("."));

        final ForkJoinTask<Values> taskFuture = this.forkJoinPool.submit(
                template.getProcessorGraph().createTask(values));
        taskFuture.get();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("overviewMapLayerGraphics", List.class);
        assertEquals(2, layerGraphics.size());

        final BufferedImage actualImage = ImageSimilarity.mergeImages(layerGraphics, 300, 200);
        new ImageSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"))
                .assertSimilarity(actualImage, 110);
    }
}
