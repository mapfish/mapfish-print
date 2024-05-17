package org.mapfish.print.http;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

/**
 * Schedule tasks for caching Http Requests that can be run simultaneously.
 *
 * <p>The instances of the returned request will use a future to wait for the actual request to be
 * really completed.
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
      final File temporaryDirectory,
      final MetricRegistry registry,
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
      try (OutputStream os = Files.newOutputStream(this.cachedFile.toPath())) {
        IOUtils.copy(originalResponse.getBody(), os);
      }
    }

    @Override
    @Nonnull
    public InputStream getBody() throws IOException {
      if (this.body == null) {
        this.body = new FileInputStream(this.cachedFile);
      }
      return this.body;
    }

    @Override
    @Nonnull
    public HttpHeaders getHeaders() {
      return this.headers;
    }

    @Override
    public int getRawStatusCode() {
      return this.status;
    }

    @Override
    @Nonnull
    public String getStatusText() {
      return this.statusText;
    }

    @Override
    public void close() {
      if (this.body != null) {
        try {
          this.body.close();
        } catch (IOException e) {
          LOGGER.warn("Failed to close body", e);
        } finally {
          this.body = null;
        }
      }
    }
  }

  private final class CachedClientHttpRequest implements ClientHttpRequest, Callable<Void> {
    private final ClientHttpRequest originalRequest;
    private final Processor.ExecutionContext context;
    @Nullable private ClientHttpResponse response;
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
    @Nonnull
    public String getMethodValue() {
      final HttpMethod method = getMethod();
      return method != null ? method.name() : "";
    }

    @Override
    @Nonnull
    public URI getURI() {
      return this.originalRequest.getURI();
    }

    @Override
    @Nonnull
    public HttpHeaders getHeaders() {
      return this.originalRequest.getHeaders();
    }

    @Override
    @Nonnull
    public OutputStream getBody() {
      // body should be written before creating this object
      throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public ClientHttpResponse execute() {
      assert this.future != null;
      Timer timerWait = HttpRequestFetcher.this.registry.timer(buildMetricName(".waitDownloader"));
      try (Timer.Context ignored = timerWait.time()) {
        this.future.join();
      }
      assert this.response != null;
      LOGGER.debug("Loading cached URI resource {}", this.originalRequest.getURI());

      // Drop the reference to the response to save some memory. It is wrong to call execute
      // twice...
      final ClientHttpResponse result = this.response;
      this.response = null;
      this.future = null;
      return result;
    }

    private String buildMetricName(final String suffix) {
      return HttpRequestFetcher.class.getName() + suffix;
    }

    @Override
    public Void call() throws Exception {
      return context.mdcContextEx(
          () -> {
            final String baseMetricName =
                buildMetricName(".read." + StatsUtils.quotePart(getURI().getHost()));
            Timer timerDownload = HttpRequestFetcher.this.registry.timer(baseMetricName);
            try (Timer.Context ignored = timerDownload.time()) {
              try {
                context.stopIfCanceled();
                this.response = new CachedClientHttpResponse(this.originalRequest.execute());
              } catch (IOException | RuntimeException e) {
                LOGGER.error("Request failed {}", this.originalRequest.getURI(), e);
                this.response = new ErrorResponseClientHttpResponse(e);
                HttpRequestFetcher.this.registry.counter(baseMetricName + ".error").inc();
              }
            }
            return null;
          });
    }

    public void setFuture(final @Nonnull ForkJoinTask<Void> future) {
      this.future = future;
    }
  }
}
