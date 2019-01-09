package org.mapfish.print.processor.map.scalebar;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Basic test of the Scalebar processor.
 */
public class CreateScaleBarProcessorFixedScaleCenterOsmTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_osm_fixedscale/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory requestFactory;

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateScaleBarProcessorFixedScaleCenterOsmTest.class,
                                       BASE_DIR + "requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "center_osm_fixedscale";
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
        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(2, layerGraphics.size());

        final BufferedImage referenceImage = ImageSimilarity.mergeImages(layerGraphics, 780, 330);

        new ImageSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"))
                .assertSimilarity(referenceImage, 50);

        String scalebarGraphic = values.getObject("scalebarGraphic", String.class);

        new ImageSimilarity(getFile(BASE_DIR + "expectedScalebar.png")).assertSimilarity(
                new File(new URI(scalebarGraphic)), 420);
        assertNotNull(values.getObject("scalebarSubReport", String.class));

        // now without a subreport
        final Configuration config_noreport = configurationFactory.getConfig(
                getFile(BASE_DIR + "config-no-report.yaml"));
        final Template template_noreport = config_noreport.getTemplate("main");
        Values values_noreport = new Values("test", requestData, template_noreport,
                                            getTaskDirectory(), this.requestFactory, new File("."));
        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(values_noreport));

        assertNull(values_noreport.getObject("scalebarSubReport", String.class));
    }
}
