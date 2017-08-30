package org.mapfish.print.processor.http;

import com.google.common.base.Predicate;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.HttpRequestHeadersAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import javax.annotation.Nullable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/http/forward-headers/add-custom-processor-application-context.xml"
})
public class ForwardHeadersProcessorTest extends AbstractHttpProcessorTest {

    @Override
    protected String baseDir() {
        return "forward-headers";
    }

    @Override
    protected Class<TestProcessor> testProcessorClass() {
        return TestProcessor.class;
    }

    @Override
    protected Class<? extends HttpProcessor> classUnderTest() {
        return ForwardHeadersProcessor.class;
    }

    @Override
    protected void addExtraValues(Values values) throws JSONException {
        HttpRequestHeadersAttribute.Value headers = new HttpRequestHeadersAttribute.Value();
        JSONObject inner = new JSONObject("{\"header1\": [\"header1-v1\",\"header1-v2\"], \"header2\": [\"header2-value\"], \"header3" +
                                          "\": [\"header3-value\"]}");
        headers.requestHeaders = new PJsonObject(inner, "headers");
        values.put("requestHeaders", headers);
    }

    @Test
    @DirtiesContext
    public void testForwardAll() throws Exception {
        this.httpClientFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return true;
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                return new MockClientHttpRequest(httpMethod, uri);
            }
        });

        configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(baseDir() + "/config-forward-all.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();

        Values values = new Values();
        values.put(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, new MfClientHttpRequestFactoryProvider(this.httpClientFactory));
        addExtraValues(values);
        forkJoinPool.invoke(graph.createTask(values));
    }

    public static class TestProcessor extends AbstractTestProcessor {
        @Nullable
        @Override
        public Void execute(TestParam values, ExecutionContext context) throws Exception {
            final URI uri = new URI("http://localhost:8080/path?query#fragment");
            final ClientHttpRequest request = values.clientHttpRequestFactoryProvider.get().createRequest(uri,
                    HttpMethod.GET);
            final URI finalUri = request.getURI();

            assertEquals("http", finalUri.getScheme());
            assertEquals("localhost", finalUri.getHost());
            assertEquals("/path", finalUri.getPath());
            assertEquals(8080, finalUri.getPort());
            assertEquals("query", finalUri.getQuery());
            assertEquals("fragment", finalUri.getFragment());

            assertEquals(2, request.getHeaders().size());
            assertArrayEquals(new Object[]{"header1-v1", "header1-v2"}, request.getHeaders().get("header1").toArray());
            assertArrayEquals(new Object[]{"header2-value"}, request.getHeaders().get("header2").toArray());
            return null;
        }
    }
    public static class TestProcessor2 extends AbstractTestProcessor {
        @Nullable
        @Override
        public Void execute(TestParam values, ExecutionContext context) throws Exception {
            final URI uri = new URI("http://localhost:8080/path?query#fragment");
            final ClientHttpRequest request = values.clientHttpRequestFactoryProvider.get().createRequest(uri,
                    HttpMethod.GET);

            assertEquals(3, request.getHeaders().size());
            assertArrayEquals(new Object[]{"header1-v1", "header1-v2"}, request.getHeaders().get("header1").toArray());
            assertArrayEquals(new Object[]{"header2-value"}, request.getHeaders().get("header2").toArray());
            assertArrayEquals(new Object[]{"header3-value"}, request.getHeaders().get("header3").toArray());
            return null;
        }
    }
}
