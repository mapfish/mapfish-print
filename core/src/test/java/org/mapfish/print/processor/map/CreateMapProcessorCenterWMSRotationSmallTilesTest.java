package org.mapfish.print.processor.map;

import com.google.common.collect.Multimap;
import org.junit.Test;
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
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests map rotation with small tiles. Bounds when getting tiles have to take rotation into account.
 */
public class CreateMapProcessorCenterWMSRotationSmallTilesTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_wms_rotation_small_tiles";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorCenterWMSRotationSmallTilesTest.class,
                                       BASE_DIR + "/requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        requestFactory.registerHandler(
                input -> {
                    final String host = BASE_DIR + ".com";
                    return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
                },
                new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod)
                            throws Exception {
                        final Multimap<String, String> parameters = URIUtils.getParameters(uri);

                        try {
                            byte[] bytes =
                                    Files.readAllBytes(
                                            getTile(parameters.get("bbox").iterator().next()).toPath());
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
        );

        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "/config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(),
                                   this.requestFactory, new File("."));
        template.getProcessorGraph().createTask(values).invoke();

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(1, layerGraphics.size());

        new ImageSimilarity(getFile(BASE_DIR + "/expectedSimpleImage.png"))
                .assertSimilarity(layerGraphics, 625, 625, 1);
    }

    private File getTile(String bbox) {
        boolean oddEven = keyHash(bbox) % 2 == 0;
        if (oddEven) {
            return getFile(BASE_DIR + "/red_square.png");
        } else {
            return getFile(BASE_DIR + "/green_square.png");
        }
    }

    private int keyHash(String key) {
        int k = key.length();
        int u = 0;
        int n;

        for (int i = 0; i < k; i++) {
            n = (int) key.charAt(i);
            u += i * n % 31;
        }
        return u % 139;
    }
}
