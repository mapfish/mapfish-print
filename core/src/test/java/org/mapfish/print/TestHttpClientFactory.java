package org.mapfish.print;


import org.apache.http.client.methods.HttpRequestBase;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigurableRequest;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.junit.Assert.fail;

/**
 * Allows tests to provide canned responses to requests.
 */
public class TestHttpClientFactory extends MfClientHttpRequestFactoryImpl
        implements MfClientHttpRequestFactory {

    private final Map<Predicate<URI>, Handler> handlers = new ConcurrentHashMap<>();

    public TestHttpClientFactory() {
        super(20, 10);
    }

    public void registerHandler(Predicate<URI> matcher, Handler handler) {
        if (handlers.containsKey(matcher)) {
            throw new IllegalArgumentException(matcher + " has already been registered");
        }
        handlers.put(matcher, handler);
    }

    @Override
    public ConfigurableRequest createRequest(URI uri, final HttpMethod httpMethod) {
        for (Map.Entry<Predicate<URI>, Handler> entry: handlers.entrySet()) {
            if (entry.getKey().test(uri)) {
                try {
                    final MockClientHttpRequest httpRequest = entry.getValue().handleRequest(uri, httpMethod);
                    return new TestConfigurableRequest(httpRequest);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalArgumentException(uri + " not registered with " + getClass().getName());
    }

    @Override
    public void register(RequestConfigurator callback) {
        throw new UnsupportedOperationException("Not supported");
    }

    public static abstract class Handler {
        public abstract MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception;

        public MockClientHttpRequest ok(URI uri, byte[] bytes, HttpMethod httpMethod) {
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
            ClientHttpResponse response = new MockClientHttpResponse(bytes, HttpStatus.OK);
            request.setResponse(response);
            return request;
        }

        public MockClientHttpRequest error404(URI uri, HttpMethod httpMethod) {
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
            MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.NOT_FOUND);
            request.setResponse(response);
            return request;
        }

        public MockClientHttpRequest failOnExecute(final URI uri, final HttpMethod httpMethod) {
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() throws IOException {
                    fail("request should not be executed " + uri.toString());
                    throw new IOException();
                }
            };
            return request;
        }
    }

    private static class TestConfigurableRequest implements ConfigurableRequest {
        private final MockClientHttpRequest httpRequest;

        public TestConfigurableRequest(MockClientHttpRequest httpRequest) {
            this.httpRequest = httpRequest;
        }

        @Override
        public HttpRequestBase getUnderlyingRequest() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public void setConfiguration(Configuration configuration) {
            // ignore
        }

        @Override
        public ClientHttpResponse execute() throws IOException {
            return httpRequest.execute();
        }

        @Override
        public OutputStream getBody() throws IOException {
            return httpRequest.getBody();
        }

        @Override
        public HttpMethod getMethod() {
            return httpRequest.getMethod();
        }

        @Override
        public String getMethodValue() {
            final HttpMethod method = httpRequest.getMethod();
            assert method != null;
            return method.name();
        }

        @Override
        public URI getURI() {
            return httpRequest.getURI();
        }

        @Override
        public HttpHeaders getHeaders() {
            return httpRequest.getHeaders();
        }
    }
}
