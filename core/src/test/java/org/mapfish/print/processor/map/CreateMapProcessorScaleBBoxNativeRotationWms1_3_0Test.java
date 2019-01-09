package org.mapfish.print.processor.map;

import com.google.common.collect.HashMultimap;
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
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for native rotation on WMS layers.
 */
public class CreateMapProcessorScaleBBoxNativeRotationWms1_3_0Test extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "bbox_native_rotation_wms1_3_0_scale/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;

    @Autowired
    private ForkJoinPool forkJoinPool;

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorScaleBBoxNativeRotationWms1_3_0Test.class,
                                       BASE_DIR + "requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "bbox_native_rotation_wms1_3_0_scale";
        requestFactory.registerHandler(
                input -> (("" + input.getHost()).contains(host + ".wms")) ||
                        input.getAuthority().contains(host + ".wms"),
                createFileHandler(uri -> {
                    final Multimap<String, String> uppercaseParams = HashMultimap.create();
                    for (Map.Entry<String, String> entry: URIUtils.getParameters(uri).entries()) {
                        uppercaseParams.put(entry.getKey().toUpperCase(), entry.getValue().toUpperCase());
                    }

                    assertTrue("SERVICE != WMS: " + uppercaseParams.get("WMS"),
                               uppercaseParams.containsEntry("SERVICE", "WMS"));
                    assertTrue("FORMAT != IMAGE/PNG: " + uppercaseParams.get("FORMAT"),
                               uppercaseParams.containsEntry("FORMAT", "IMAGE/PNG"));
                    assertTrue("REQUEST != GETMAP: " + uppercaseParams.get("REQUEST"),
                               uppercaseParams.containsEntry("REQUEST", "GETMAP"));
                    assertTrue("VERSION != 1.3.0: " + uppercaseParams.get("VERSION"),
                               uppercaseParams.containsEntry("VERSION", "1.3.0"));
                    assertTrue("LAYERS != TOPP:STATES: " + uppercaseParams.get("LAYERS"),
                               uppercaseParams.containsEntry("LAYERS", "TOPP:STATES"));
                    assertTrue("ANGLE != 90", uppercaseParams.containsEntry("ANGLE", "90.0"));
                    assertTrue("BBOX is missing", uppercaseParams.containsKey("BBOX"));
                    assertTrue("mapSize is not rotated (width)",
                               uppercaseParams.containsEntry("WIDTH", "780"));
                    assertTrue("mapSize is not rotated (height)",
                               uppercaseParams.containsEntry("HEIGHT", "330"));
                    return "/map-data/states-native-rotation.png";
                })
        );
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(),
                                   this.requestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(1, layerGraphics.size());

        new ImageSimilarity(new File(layerGraphics.get(0)))
                .assertSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"), 1);

    }
}
