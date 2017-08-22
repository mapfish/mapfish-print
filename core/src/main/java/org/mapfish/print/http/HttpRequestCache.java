package org.mapfish.print.http;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

/**
 *
 * Creates tasks for caching Http Requests that can be run simultaneously.
 *
 */
public final class HttpRequestCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestCache.class);

    private final List<CachedClientHttpRequest> requests = new ArrayList<CachedClientHttpRequest>();

    private final File temporaryDirectory;

    private final MetricRegistry registry;

    private boolean cached = false;

    private class CachedClientHttpResponse extends AbstractClientHttpResponse {

        private final File cachedFile;
        private final HttpHeaders headers;
        private final int status;
        private final String statusText;
        private InputStream body;

        public CachedClientHttpResponse(final ClientHttpResponse originalResponse) throws IOException {
            this.headers = originalResponse.getHeaders();
            this.status = originalResponse.getRawStatusCode();
            this.statusText = originalResponse.getStatusText();
            this.cachedFile = File.createTempFile("cacheduri", null, HttpRequestCache.this.temporaryDirectory);
            InputStream is = originalResponse.getBody();
            try {
                OutputStream os = new FileOutputStream(this.cachedFile);
                try {
                    IOUtils.copy(is, os);
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
        }

        @Override
        public InputStream getBody() throws IOException {
            if (this.body == null) {
                this.body = new FileInputStream(this.cachedFile);
            }
            return this.body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return this.status;
        }

        @Override
        public String getStatusText() throws IOException {
            return this.statusText;
        }

        @Override
        public void close() {
            if (this.body != null) {
                try {
                    this.body.close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private class CachedClientHttpRequest implements ClientHttpRequest, Callable<Void> {
        private final ClientHttpRequest originalRequest;
        private CachedClientHttpResponse response;

        public CachedClientHttpRequest(final ClientHttpRequest request) throws IOException {
            this.originalRequest = request;
        }

        @Override
        public HttpMethod getMethod() {
            return this.originalRequest.getMethod();
        }

        @Override
        public URI getURI() {
            return this.originalRequest.getURI();
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.originalRequest.getHeaders();
        }

        @Override
        public OutputStream getBody() throws IOException {
            //body should be written before creating this object
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientHttpResponse execute() throws IOException {
            if (!HttpRequestCache.this.cached) {
                LOGGER.warn("Attempting to load cached URI before actual caching: " + this.originalRequest.getURI());
            } else if (this.response == null) {
                LOGGER.warn("Attempting to load cached URI from failed request: " + this.originalRequest.getURI());
            } else {
                LOGGER.debug("Loading cached URI resource " + this.originalRequest.getURI());
            }
            return this.response;
        }

        @Override
        public Void call() throws Exception {
            final String baseMetricName = HttpRequestCache.class.getName() + ".read." + getURI().getHost();
            final Timer.Context timerDownload = HttpRequestCache.this.registry.timer(baseMetricName).time();
            ClientHttpResponse originalResponse = null;
            try {
                originalResponse = this.originalRequest.execute();
                LOGGER.debug("Caching URI resource " + this.originalRequest.getURI());
                this.response = new CachedClientHttpResponse(originalResponse);
            } catch (IOException e) {
                LOGGER.error("Request failed " + this.originalRequest.getURI(), e);
                HttpRequestCache.this.registry.counter(baseMetricName + ".error").inc();
                throw e;
            } finally {
                if (originalResponse != null) {
                    originalResponse.close();
                }
                timerDownload.stop();
            }
            return null;
        }
    }

    /**
     * Constructor.
     *
     * @param temporaryDirectory temporary directory for cached requests
     * @param registry the metric registry
     */
    public HttpRequestCache(final File temporaryDirectory, final MetricRegistry registry) {
        this.temporaryDirectory = temporaryDirectory;
        this.registry = registry;
    }

    private CachedClientHttpRequest save(final CachedClientHttpRequest request) {
        this.requests.add(request);
        return request;
    }

    /**
     * Register a http request for caching. Returns a handle to the HttpRequest that will be cached.
     *
     * @param originalRequest the original request
     * @return the cached http request
     * @throws IOException
     */
    public ClientHttpRequest register(final ClientHttpRequest originalRequest) throws IOException {
        return save(new CachedClientHttpRequest(originalRequest));
    }

    /**
     * Register a URI for caching. Returns a handle to the HttpRequest that will be cached.
     *
     * @param factory the request factory
     * @param uri the uri
     * @return the cached http request
     * @throws IOException
     */
    public ClientHttpRequest register(final MfClientHttpRequestFactory factory, final URI uri) throws IOException {
        return register(factory.createRequest(uri, HttpMethod.GET));
    }

    /**
     * Cache all requests at once.
     *
     * @param requestForkJoinPool request fork join pool
     */
    public void cache(final ForkJoinPool requestForkJoinPool) {
        if (!this.cached) {
            requestForkJoinPool.invokeAll(this.requests);
            this.cached = true;
        } else {
            LOGGER.warn("Attempting to cache twice!");
        }
    }
}
