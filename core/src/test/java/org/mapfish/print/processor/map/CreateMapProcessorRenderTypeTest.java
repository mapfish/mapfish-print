package org.mapfish.print.processor.map;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateMapProcessorRenderTypeTest extends AbstractMapfishSpringTest {

    public static final String BASE_DIR = "rendertype/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorAoiTest.class, BASE_DIR + "requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "center_wms1_0_0_flexiblescale";
        requestFactory.registerHandler(
                input -> (("" + input.getHost()).contains(host + ".wms")) ||
                        input.getAuthority().contains(host + ".wms"),
                createFileHandler("/map-data/zoomed-in-ny-tiger.tif")
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
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);

        assertEquals(4, layerGraphics.size());
        assertTrue(layerGraphics.get(0).getPath().endsWith(".jpeg"));
        assertTrue(layerGraphics.get(1).getPath().endsWith(".png"));
        assertTrue(layerGraphics.get(2).getPath().endsWith(".svg"));
        assertTrue(layerGraphics.get(3).getPath().endsWith(".png"));

    }
}
