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
import java.util.concurrent.ForkJoinPool;

public class CreateNorthArrowProcessorSvgTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "north_arrow_svg/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFixedScaleCenterOsmTest.class,
                                       BASE_DIR + "requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "north_arrow";
        requestFactory.registerHandler(
                input -> (("" + input.getHost()).contains(host + ".osm")) ||
                        input.getAuthority().contains(host + ".osm"),
                createFileHandler(uri -> "/map-data/osm" + uri.getPath())
        );

        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(),
                                   this.requestFactory, new File("."));
        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        String northArrowGraphic = values.getObject("northArrowGraphic", String.class);

        new ImageSimilarity(getFile(BASE_DIR + "expectedNorthArrow.png"))
                .assertSimilarity(new URI(northArrowGraphic), 200, 200, 75);
    }
}
