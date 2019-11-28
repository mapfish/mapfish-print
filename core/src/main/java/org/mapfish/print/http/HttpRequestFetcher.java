package org.mapfish.print.http;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.commons.io.IOUtils;
import org.mapfish.print.StatsUtils;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import javax.annotation.Nullable;

/**
 * Schedule tasks for caching Http Requests that can be run simultaneously.
 * <p>
 * The instances of the returned request will use a future to wait for the actual request to be really
 * completed.
 */
public final class HttpRequestFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestFetcher.class);

    private final File temporaryDirectory;

    private final MetricRegistry registry;
    private final Processor.ExecutionContext context;
    private final ForkJoinPool requestForkJoinPool;

    /**
     * Constructor.
     *
     * @param temporaryDirectory temporary directory for cached requests
     * @param registry the metric registry
     * @param context the job ID
     * @param requestForkJoinPool the work pool to use to do the requests
     */
    public HttpRequestFetcher(
            final File temporaryDirectory, final MetricRegistry registry,
            final Processor.ExecutionContext context,
            final ForkJoinPool requestForkJoinPool) {
        this.temporaryDirectory = temporaryDirectory;
        this.registry = registry;
        this.context = context;
        this.requestForkJoinPool = requestForkJoinPool;
    }

    private CachedClientHttpRequest add(final CachedClientHttpRequest request) {
        final ForkJoinTask<Void> future = this.requestForkJoinPool.submit(request);
        request.setFuture(future);
        return request;
    }

    /**
     * Register a http request for caching. Returns a handle to the HttpRequest that will be cached.
     *
     * @param originalRequest the original request
     * @return the cached http request
     */
    public ClientHttpRequest register(final ClientHttpRequest originalRequest) {
        return add(new CachedClientHttpRequest(originalRequest, this.context));
    }

    private final class CachedClientHttpResponse extends AbstractClientHttpResponse {

        private final File cachedFile;
        private final HttpHeaders headers;
        private final int status;
        private final String statusText;
        private InputStream body;

        private CachedClientHttpResponse(final ClientHttpResponse originalResponse) throws IOException {
            this.headers = originalResponse.getHeaders();
            this.status = originalResponse.getRawStatusCode();
            this.statusText = originalResponse.getStatusText();
            this.cachedFile =
                    File.createTempFile("cacheduri", null, HttpRequestFetcher.this.temporaryDirectory);
            try (OutputStream os = new FileOutputStream(this.cachedFile)) {
                IOUtils.copy(originalResponse.getBody(), os);
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
        public int getRawStatusCode() {
            return this.status;
        }

        @Override
        public String getStatusText() {
            return this.statusText;
        }

        @Override
        public void close() {
            if (this.body != null) {
                try {
                    this.body.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    private final class CachedClientHttpRequest implements ClientHttpRequest, Callable<Void> {
        private final ClientHttpRequest originalRequest;
        private final Processor.ExecutionContext context;
        @Nullable
        private ClientHttpResponse response;
        @Nullable
        private ForkJoinTask<Void> future;

        private CachedClientHttpRequest(
                final ClientHttpRequest request, final Processor.ExecutionContext context) {
            this.originalRequest = request;
            this.context = context;
        }

        @Override
        public HttpMethod getMethod() {
            return this.originalRequest.getMethod();
        }

        @Override
        public String getMethodValue() {
            final HttpMethod method = this.originalRequest.getMethod();
            return method != null ? method.name() : "";
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
        public OutputStream getBody() {
            //body should be written before creating this object
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientHttpResponse execute() {
            assert this.future != null;
            final Timer.Context timerWait =
                    HttpRequestFetcher.this.registry.timer(HttpRequestFetcher.class.getName() +
                                                                   ".waitDownloader").time();
            this.future.join();
            timerWait.stop();
            assert this.response != null;
            LOGGER.debug("Loading cached URI resource {}", this.originalRequest.getURI());

            // Drop the reference to the response to save some memory. It is wrong to call execute twice...
            final ClientHttpResponse result = this.response;
            this.response = null;
            this.future = null;
            return result;
        }

        @Override
        public Void call() throws Exception {
            return context.mdcContextEx(() -> {
                final String baseMetricName =
                        HttpRequestFetcher.class.getName() + ".read." +
                                StatsUtils.quotePart(getURI().getHost());
                final Timer.Context timerDownload =
                        HttpRequestFetcher.this.registry.timer(baseMetricName).time();
                try (ClientHttpResponse originalResponse = this.originalRequest.execute()) {
                    context.stopIfCanceled();
                    this.response = new CachedClientHttpResponse(originalResponse);
                } catch (IOException e) {
                    LOGGER.warn("Request failed {}", this.originalRequest.getURI(), e);
                    this.response = new AbstractClientHttpResponse() {
                        @Override
                        public HttpHeaders getHeaders() {
                            return new HttpHeaders();
                        }

                        @Override
                        public InputStream getBody() {
                            return StreamUtils.emptyInput();
                        }

                        @Override
                        public int getRawStatusCode() {
                            return 500;
                        }

                        @Override
                        public String getStatusText() {
                            return e.getMessage();
                        }

                        @Override
                        public void close() {
                        }
                    };
                    HttpRequestFetcher.this.registry.counter(baseMetricName + ".error").inc();
                } finally {
                    timerDownload.stop();
                }
                return null;
            });
        }

        public void setFuture(final ForkJoinTask<Void> future) {
            this.future = future;
        }
    }
}
