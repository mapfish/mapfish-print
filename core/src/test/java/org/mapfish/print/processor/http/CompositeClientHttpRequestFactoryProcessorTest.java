package org.mapfish.print.processor.http;

import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.attribute.HttpRequestHeadersAttribute;
import org.mapfish.print.output.Values;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import javax.annotation.Nullable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/http/composite-client-http-request-factory/add-custom" +
                "-processor-application-context.xml"
})
public class CompositeClientHttpRequestFactoryProcessorTest extends AbstractHttpProcessorTest {

    @Override
    protected String baseDir() {
        return "composite-client-http-request-factory";
    }

    @Override
    protected Class<TestProcessor> testProcessorClass() {
        return TestProcessor.class;
    }

    @Override
    protected Class<CompositeClientHttpRequestFactoryProcessor> classUnderTest() {
        return CompositeClientHttpRequestFactoryProcessor.class;
    }

    @Override
    protected void addExtraValues(Values values) throws JSONException {
        HttpRequestHeadersAttribute.Value headers = new HttpRequestHeadersAttribute.Value();
        JSONObject inner = new JSONObject("{\"header1\": [\"value\"]}");
        headers.requestHeaders = new PJsonObject(inner, "headers");
        values.put("requestHeaders", headers);
    }

    public static class TestProcessor extends AbstractTestProcessor {

        @Nullable
        @Override
        public Void execute(TestParam values, ExecutionContext context) throws Exception {
            final URI uri = new URI("https://localhost:8443/path?query#fragment");
            final ClientHttpRequest request = values.clientHttpRequestFactoryProvider.get().createRequest(uri,
                                                                                                          HttpMethod.GET);
            final URI finalUri = request.getURI();

            assertEquals("http", finalUri.getScheme());
            assertEquals("127.0.0.1", finalUri.getHost());
            assertEquals("/path", finalUri.getPath());
            assertEquals(9999, finalUri.getPort());
            assertEquals("query", finalUri.getQuery());
            assertEquals("fragment", finalUri.getFragment());

            assertEquals(1, request.getHeaders().size());
            assertArrayEquals(new Object[]{"value"}, request.getHeaders().get("header1").toArray());

            return null;
        }
    }
}
