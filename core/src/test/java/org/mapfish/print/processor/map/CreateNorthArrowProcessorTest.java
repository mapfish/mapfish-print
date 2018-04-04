package org.mapfish.print.processor.map;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import java.util.concurrent.ForkJoinPool;
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
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class CreateNorthArrowProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "north_arrow/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "north_arrow";
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host + ".osm")) ||
                                input.getAuthority().contains(host + ".osm");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data/osm" + uri.getPath()));
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
        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        String northArrowGraphic = values.getObject("northArrowGraphic", String.class);

        new ImageSimilarity(new File(new URI(northArrowGraphic)))
                .assertSimilarity(getFile(BASE_DIR + "expectedNorthArrow.png"), 1);

        assertNotNull(values.getObject("northArrowOut", String.class));

        //now without a subreport
        final Configuration configNoReport = configurationFactory.getConfig(
                getFile(BASE_DIR + "config-no-report.yaml"));
        final Template templateNoReport = configNoReport.getTemplate("main");
        Values valuesNoReport = new Values("test", requestData, templateNoReport, getTaskDirectory(),
                this.requestFactory, new File("."));
        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(valuesNoReport));

        assertNull(valuesNoReport.getObject("northArrowOut", String.class));
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFixedScaleCenterOsmTest.class,
                BASE_DIR + "requestData.json");
    }
}
