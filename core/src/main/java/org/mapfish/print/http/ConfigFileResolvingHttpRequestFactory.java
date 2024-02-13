package org.mapfish.print.http;

import org.locationtech.jts.util.Assert;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.url.data.DataUrlConnection;
import org.mapfish.print.url.data.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${httpRequest.fetchRetry.maxNumber}")
    private int httpRequestMaxNumberFetchRetry;

    @Value("${httpRequest.fetchRetry.intervalMillis}")
    private int httpRequestFetchRetryIntervalMillis;


    /**
     * Constructor.
     *
     * @param httpRequestFactory basic request factory
     * @param config the template for the current print job.
     * @param jobId the job ID
     */
    public ConfigFileResolvingHttpRequestFactory(
            final MfClientHttpRequestFactoryImpl httpRequestFactory,
            final Configuration config, final String jobId,
            final int httpRequestMaxNumberFetchRetry,
            final int httpRequestFetchRetryIntervalMillis) {
        this.httpRequestFactory = httpRequestFactory;
        this.config = config;
        this.jobId = jobId;
        this.httpRequestMaxNumberFetchRetry = httpRequestMaxNumberFetchRetry;
        this.httpRequestFetchRetryIntervalMillis = httpRequestFetchRetryIntervalMillis;
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

                if ("data".equals(this.uri.getScheme())) {
                    return doDataUriRequest();
                }
                if (
                    getURI().getScheme() == null
                    || Arrays.asList("file", "", "servlet", "classpath").contains(getURI().getScheme())
                ) {
                    return doFileRequest();
                }
                return doHttpRequestWithRetry(headers);
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

        private ClientHttpResponse doDataUriRequest() throws IOException {
            final String urlStr = this.uri.toString();
            final URL url = new URL("data", null, 0,
                urlStr.substring("data:".length()), new Handler());
            final DataUrlConnection duc = new DataUrlConnection(url);
            final InputStream is = duc.getInputStream();
            final String contentType = duc.getContentType();
            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("Content-Type", contentType);
            final ConfigFileResolverHttpResponse response =
            new ConfigFileResolverHttpResponse(is, responseHeaders);
            LOGGER.debug("Resolved request using DataUrlConnection: {}", contentType);
            return response;
        }

        private ClientHttpResponse doFileRequest() throws IOException {
            final String uriString = this.uri.toString();
            final Configuration configuration = ConfigFileResolvingHttpRequestFactory.this.config;
            final byte[] bytes = configuration.loadFile(uriString);
            final InputStream is = new ByteArrayInputStream(bytes);
            final HttpHeaders responseHeaders = new HttpHeaders();
            final Optional<File> file = configuration.getFile(uriString);
            if (file.isPresent()) {
                responseHeaders.set(
                    "Content-Length", String.valueOf(Files.probeContentType(file.get().toPath()))
                );
            }
            final ConfigFileResolverHttpResponse response =
                    new ConfigFileResolverHttpResponse(is, responseHeaders);
            LOGGER.debug("Resolved request: {} using MapFish print config file loaders.",
                            uriString);
            return response;
        }

        private ClientHttpResponse doHttpRequestWithRetry(final HttpHeaders headers) throws IOException {
            AtomicInteger counter = new AtomicInteger();
            do {
                try {
                    // Display headers, one by line <name>: <value>
                    LOGGER.debug("Fetching URI resource {}, headers:\n{}", this.getURI(),
                        headers.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + String.join(", ", entry.getValue()))
                            .collect(Collectors.joining("\n"))
                    );
                    ClientHttpRequest requestUsed = this.request != null ? this.request :
                        createRequestFromWrapped(headers);
                    LOGGER.debug("Executing http request: {}", requestUsed.getURI());
                    ClientHttpResponse response = executeCallbacksAndRequest(requestUsed);
                    if (response.getRawStatusCode() < 500) {
                        LOGGER.debug("Fetching success URI resource {}, error code {}",
                                    getURI(), response.getRawStatusCode());
                        return response;
                    }
                    LOGGER.debug("Fetching failed URI resource {}, error code {}",
                                getURI(), response.getRawStatusCode());
                    if (counter.incrementAndGet() < httpRequestMaxNumberFetchRetry) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(httpRequestFetchRetryIntervalMillis);
                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                        LOGGER.debug("Retry fetching URI resource {}", this.getURI());
                    } else {
                        throw new RuntimeException(String .format(
                            "Fetching failed URI resource %s, error code %s",
                            getURI(), response.getRawStatusCode()
                        ));
                    }
                } catch (final IOException e) {
                    if (counter.incrementAndGet() < httpRequestMaxNumberFetchRetry) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(httpRequestFetchRetryIntervalMillis);
                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                        LOGGER.debug("Retry fetching URI resource {}", this.getURI());
                    } else {
                        LOGGER.debug("Fetching failed URI resource {}", getURI());
                        throw e;
                    }
                }
            } while (true);
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
            private final InputStream is;
            private final HttpHeaders headers;

            ConfigFileResolverHttpResponse(
                    final InputStream is,
                    final HttpHeaders headers) {
                this.headers = headers;
                this.is = is;
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
                return this.is;
            }

            @Override
            public HttpHeaders getHeaders() {
                return this.headers;
            }
        }
    }
}
