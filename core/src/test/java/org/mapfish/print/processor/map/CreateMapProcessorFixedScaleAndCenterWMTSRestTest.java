package org.mapfish.print.processor.map;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.Constants.PDF_DPI;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.MapfishMapContext;
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

import com.google.common.base.Predicate;
import com.google.common.io.Files;

/**
 * Basic test of the Map processor.
 * <p></p>
 * Created by Jesse on 3/26/14.
 */
public class CreateMapProcessorFixedScaleAndCenterWMTSRestTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "center_wmts_fixedscale_rest/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        final String host = "center_wmts_fixedscale_rest.com";
                        return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        Pattern pattern = Pattern.compile(".*\\/([0-9]+)\\/([0-9]+).*");
                        Matcher matcher = pattern.matcher(uri.toString());
                        if (!matcher.matches()) {
                            return error404(uri, httpMethod);
                        }
                        String column = matcher.group(1);
                        String row = matcher.group(2);
                        try {
                            byte[] bytes = Files.toByteArray(
                                    getFile("/map-data/ny-tiles/" + column + "x" + row + ".png"));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
        );
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        final String host = "center_wmts_fixedscale_rest.json";
                        return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data" + uri.getPath()));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
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
        assertEquals(2, layerGraphics.size());

        MapfishMapContext mapContext = values.getObject("mapContext", MapfishMapContext.class);
        assertEquals(110000.0, mapContext.getScale().getDenominator(PDF_DPI), 1E-6);

        new ImageSimilarity(getFile(BASE_DIR + "expectedSimpleImage.png"))
                .assertSimilarity(layerGraphics, 630, 294, 40);
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFixedScaleAndCenterWMTSRestTest.class,
                BASE_DIR + "requestData.json");
    }
}
