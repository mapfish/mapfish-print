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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Basic test of the Map processor.
 *
 * Created by MaxComse on 11/09/16.
 */
public class CreateMapProcessorImageLayerTest1 extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "image_layer_test/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorImageLayerTest1.class,
                                       BASE_DIR + "requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "image_layer_test";
        requestFactory.registerHandler(
                input -> ("" + input.getHost()).contains(host) || input.getAuthority().contains(host),
                createFileHandler("/map-data/tiger-ny.png")
        );

        final Configuration config = configurationFactory.getConfig(
                getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(),
                                   this.requestFactory, new File("."));
        template.getProcessorGraph().createTask(values).invoke();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(1, layerGraphics.size());

        new ImageSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"))
                .assertSimilarity(layerGraphics, 630, 294, 1);

    }
}
