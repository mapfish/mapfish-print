package org.mapfish.print.http;

import org.locationtech.jts.util.Assert;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

/**
 * This request factory will attempt to load resources using
 * {@link org.mapfish.print.config.Configuration#loadFile(String)}
 * and {@link org.mapfish.print.config.Configuration#isAccessible(String)} to load the resources if the http
 * method is GET and will fallback to the normal/wrapped factory to make http requests.
 */
public final class ConfigFileResolvingHttpRequestFactory implements MfClientHttpRequestFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileResolvingHttpRequestFactory.class);
    private final Configuration config;
    private final String jobId;
    private final MfClientHttpRequestFactoryImpl httpRequestFactory;
    private final List<RequestConfigurator> callbacks = new CopyOnWriteArrayList<>();

    /**
     * Constructor.
     *
     * @param httpRequestFactory basic request factory
     * @param config the template for the current print job.
     * @param jobId the job ID
     */
    public ConfigFileResolvingHttpRequestFactory(
            final MfClientHttpRequestFactoryImpl httpRequestFactory,
            final Configuration config, final String jobId) {
        this.httpRequestFactory = httpRequestFactory;
        this.config = config;
        this.jobId = jobId;
    }

    @Override
    public void register(@Nonnull final RequestConfigurator callback) {
        this.callbacks.add(callback);
    }

    @Override
    public ClientHttpRequest createRequest(
            final URI uri,
            final HttpMethod httpMethod) {
        return new ConfigFileResolvingRequest(uri, httpMethod);
    }


    private class ConfigFileResolvingRequest extends AbstractClientHttpRequest {
        private final URI uri;
        private final HttpMethod httpMethod;
        private ClientHttpRequest request;


        ConfigFileResolvingRequest(
                @Nonnull final URI uri,
                @Nonnull final HttpMethod httpMethod) {
            this.uri = uri;
            this.httpMethod = httpMethod;
        }

        @Override
        protected synchronized OutputStream getBodyInternal(final HttpHeaders headers) throws IOException {
            Assert.isTrue(this.request == null, "getBodyInternal() can only be called once.");
            this.request = createRequestFromWrapped(headers);
            return this.request.getBody();
        }

        private synchronized ClientHttpRequest createRequestFromWrapped(final HttpHeaders headers)
                throws IOException {
            final MfClientHttpRequestFactoryImpl requestFactory =
                    ConfigFileResolvingHttpRequestFactory.this.httpRequestFactory;
            ConfigurableRequest httpRequest = requestFactory.createRequest(this.uri, this.httpMethod);
            httpRequest.setConfiguration(ConfigFileResolvingHttpRequestFactory.this.config);

            httpRequest.getHeaders().putAll(headers);
            httpRequest.getHeaders().set("X-Request-ID", ConfigFileResolvingHttpRequestFactory.this.jobId);
            return httpRequest;
        }

        @Override
        protected synchronized ClientHttpResponse executeInternal(final HttpHeaders headers)
                throws IOException {
            final String prev = MDC.get(Processor.MDC_JOB_ID_KEY);
            boolean mdcChanged = prev == null || jobId.equals(prev);
            if (mdcChanged) {
                MDC.put(Processor.MDC_JOB_ID_KEY, ConfigFileResolvingHttpRequestFactory.this.jobId);
            }
            try {
                if (this.request != null) {
                    LOGGER.debug("Executing http request: {}", this.request.getURI());
                    return executeCallbacksAndRequest(this.request);
                }
                if (this.httpMethod == HttpMethod.GET) {
                    final String uriString = this.uri.toString();
                    final Configuration configuration = ConfigFileResolvingHttpRequestFactory.this.config;
                    try {
                        final byte[] bytes = configuration.loadFile(uriString);
                        final ConfigFileResolverHttpResponse response =
                                new ConfigFileResolverHttpResponse(bytes, headers);
                        LOGGER.debug("Resolved request: {} using mapfish print config file loaders.",
                                     uriString);
                        return response;
                    } catch (NoSuchElementException e) {
                        // cannot be loaded by configuration so try http
                    }
                }

                LOGGER.debug("Executing http request: {}", this.getURI());
                return executeCallbacksAndRequest(createRequestFromWrapped(headers));
            } finally {
                if (mdcChanged) {
                    if (prev != null) {
                        MDC.put(Processor.MDC_JOB_ID_KEY, prev);
                    } else {
                        MDC.remove(Processor.MDC_JOB_ID_KEY);
                    }
                }
            }
        }

        private ClientHttpResponse executeCallbacksAndRequest(final ClientHttpRequest requestToExecute)
                throws IOException {
            for (RequestConfigurator callback: ConfigFileResolvingHttpRequestFactory.this.callbacks) {
                callback.configureRequest(requestToExecute);
            }

            return requestToExecute.execute();
        }

        @Override
        public HttpMethod getMethod() {
            return this.httpMethod;
        }

        @Override
        public String getMethodValue() {
            return this.httpMethod.name();
        }

        @Override
        public URI getURI() {
            return this.uri;
        }

        private class ConfigFileResolverHttpResponse implements ClientHttpResponse {
            private final HttpHeaders headers;
            private final byte[] bytes;

            ConfigFileResolverHttpResponse(
                    final byte[] bytes,
                    final HttpHeaders headers) {
                this.headers = headers;
                this.bytes = bytes;
            }

            @Override
            public HttpStatus getStatusCode() {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() {
                return getStatusCode().value();
            }

            @Override
            public String getStatusText() {
                return "OK";
            }

            @Override
            public void close() {
                // nothing to do
            }

            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(this.bytes);
            }

            @Override
            public HttpHeaders getHeaders() {
                return this.headers;
            }
        }
    }
}
