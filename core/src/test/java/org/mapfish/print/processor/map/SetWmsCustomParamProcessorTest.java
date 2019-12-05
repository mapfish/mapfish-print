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
 * Basic test of the set param to WMS layers processor.
 *
 * Created by Jesse on 3/26/14.
 */
public class SetWmsCustomParamProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "setparamprocessor/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;

    @Autowired
    private ForkJoinPool forkJoinPool;

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFlexibleScaleCenterWms1_0_0Test.class,
                                       BASE_DIR + "requestData.json");
    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "setparamprocessor";
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
                    assertTrue("FORMAT != IMAGE/TIFF: " + uppercaseParams.get("FORMAT"),
                               uppercaseParams.containsEntry("FORMAT", "IMAGE/PNG"));
                    assertTrue("REQUEST != MAP: " + uppercaseParams.get("REQUEST"),
                               uppercaseParams.containsEntry("REQUEST", "MAP"));
                    assertTrue("VERSION != 1.0.0: " + uppercaseParams.get("VERSION"),
                               uppercaseParams.containsEntry("VERSION", "1.0.0"));
                    assertTrue("LAYERS != TIGER-NY: " + uppercaseParams.get("LAYERS"),
                               uppercaseParams.containsEntry("LAYERS", "TIGER-NY"));
                    assertTrue("STYLES != LINE: " + uppercaseParams.get("STYLES"),
                               uppercaseParams.containsEntry("STYLES", "LINE"));
                    assertTrue("CUSTOMP1 != 1: " + uri,
                               uppercaseParams.containsEntry("CUSTOMP1", "1"));
                    assertTrue("CUSTOMP2 != 2", uppercaseParams.containsEntry("CUSTOMP2", "2"));
                    assertTrue("BBOX is missing", uppercaseParams.containsKey("BBOX"));
                    assertTrue("EXCEPTIONS is missing", uppercaseParams.containsKey("EXCEPTIONS"));
                    return "/map-data/tiger-ny.png";
                })
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
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        @SuppressWarnings("unchecked")
        List<URI> layerGraphics = (List<URI>) values.getObject("layerGraphics", List.class);
        assertEquals(1, layerGraphics.size());

        new ImageSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"))
                .assertSimilarity(layerGraphics, 630, 294, 40);
    }
}
