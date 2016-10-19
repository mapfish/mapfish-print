package org.mapfish.print.processor.map;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Basic test of the Map processor.
 * <p></p>
 * Created by Jesse on 3/26/14.
 */
public class CreateMapProcessorGridFixedNumlinesPointAltLabelProjTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "grid_numlines_points_fixedscale_alt_label_proj/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private MapfishParser parser;

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "grid_numlines_points_fixedscale_alt_label_proj";
        TiledWMSUtil.registerTiledWmsHandler(requestFactory, host);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("A4 landscape");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values(requestData, template, this.parser, getTaskDirectory(), this.requestFactory, new File("."));
        template.getProcessorGraph().createTask(values).invoke();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(2, layerGraphics.size());
        final BufferedImage referenceImage = ImageSimilarity.mergeImages(layerGraphics, 780, 330);

        String imageName = getExpectedImageName("", referenceImage, BASE_DIR);
//        ImageIO.write(referenceImage, "png", new File("/tmp/expectedSimpleImage.png"));

        new ImageSimilarity(referenceImage, 2).assertSimilarity(getFile(BASE_DIR + imageName), 70);
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFlexibleScaleCenterTiledWmsTest.class, BASE_DIR + "requestData.json");
    }
}
