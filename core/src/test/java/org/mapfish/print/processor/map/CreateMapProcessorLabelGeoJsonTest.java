package org.mapfish.print.processor.map;

import org.json.JSONException;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;

/**
 * Test to check that longer labels that partially are outside the viewbox are rendered.
 */
public class CreateMapProcessorLabelGeoJsonTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR ="bbox_geojson_label_style/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private MapfishParser parser;
    @Autowired
    private MfClientHttpRequestFactoryImpl httpRequestFactory;


    @Test
    public void testExecute() throws Exception {
        PJsonObject requestData = loadJsonRequestData();
        doTest(requestData);
    }

    private void doTest(PJsonObject requestData) throws IOException, JSONException {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        Values values = new Values(requestData, template, parser, getTaskDirectory(), this.httpRequestFactory, new File("."));
        template.getProcessorGraph().createTask(values).invoke();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(1, layerGraphics.size());

        final BufferedImage img = ImageIO.read(new File(layerGraphics.get(0)));
//        ImageIO.write(img, "png", new File(TMP, getClass().getSimpleName() + ".png"));
        new ImageSimilarity(img, 2).assertSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"), 500);
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorLabelGeoJsonTest.class, BASE_DIR + "requestData.json");
    }
}
