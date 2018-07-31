package org.mapfish.print.processor.http;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/http/map-uri/add-custom-processor-application-context.xml"
})
public class MapUriProcessorTest extends AbstractHttpProcessorTest {

    @Override
    protected String baseDir() {
        return "map-uri";
    }

    @Override
    protected Class<TestProcessor> testProcessorClass() {
        return TestProcessor.class;
    }

    @Override
    protected Class<? extends AbstractClientHttpRequestFactoryProcessor> classUnderTest() {
        return MapUriProcessor.class;
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
            assertEquals("127.0.0.1", finalUri.getHost());
            assertEquals("/path", finalUri.getPath());
            assertEquals(8080, finalUri.getPort());
            assertEquals("query", finalUri.getQuery());
            assertEquals("fragment", finalUri.getFragment());

            return null;
        }
    }
}
