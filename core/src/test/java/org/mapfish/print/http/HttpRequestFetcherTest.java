package org.mapfish.print.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapfish.print.processor.AbstractProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

public class HttpRequestFetcherTest {

  private ForkJoinPool pool;

  @BeforeEach
  public void setUp() {
    this.pool = new ForkJoinPool(1);
  }

  @AfterEach
  public void tearDown() {
    this.pool.shutdownNow();
  }

  /**
   * Regression test: if caching the response to a temp file fails (e.g. the temp directory is
   * unusable), the underlying original response must still be closed so its pooled HTTP
   * connection is released. Before the fix, the exception from {@code createCachedFile} would
   * propagate out of the {@code CachedClientHttpResponse} constructor without the original
   * response ever being closed, leaking the connection.
   */
  @Test
  public void registeredRequestClosesOriginalResponseWhenCachingFails() throws Exception {
    // Given: a temp directory that does not exist, so File.createTempFile(...) inside
    // HttpRequestFetcher will fail with an IOException, mirroring the production failure.
    File nonExistentDirectory = new File("/does/not/exist/" + System.nanoTime());
    HttpRequestFetcher fetcher =
        new HttpRequestFetcher(
            nonExistentDirectory,
            new MetricRegistry(),
            new AbstractProcessor.Context(new HashMap<>(), new AtomicBoolean(false)),
            this.pool);

    ClientHttpResponse originalResponse = mock(ClientHttpResponse.class);
    when(originalResponse.getHeaders()).thenReturn(new HttpHeaders());
    when(originalResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
    when(originalResponse.getStatusText()).thenReturn("OK");
    when(originalResponse.getBody()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    ClientHttpRequest originalRequest = mock(ClientHttpRequest.class);
    when(originalRequest.getURI()).thenReturn(java.net.URI.create("http://example.invalid/wms"));
    when(originalRequest.execute()).thenReturn(originalResponse);

    // When
    ClientHttpRequest cachedRequest = fetcher.register(originalRequest);
    ClientHttpResponse result = cachedRequest.execute();

    // Then: the caller gets an ErrorResponseClientHttpResponse back instead of a thrown
    // exception (this is HttpRequestFetcher's existing behaviour) ...
    assertEquals(HttpStatusCode.valueOf(406), result.getStatusCode());
    // ... and, crucially, the leaked connection is released.
    verify(originalResponse, times(1)).close();
  }

  @Test
  public void registeredRequestDoesNotCloseOriginalResponseOnSuccess() throws Exception {
    // Given: a working temp directory, so caching succeeds normally.
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    HttpRequestFetcher fetcher =
        new HttpRequestFetcher(
            tempDir,
            new MetricRegistry(),
            new AbstractProcessor.Context(new HashMap<>(), new AtomicBoolean(false)),
            this.pool);

    InputStream body = new ByteArrayInputStream(new byte[] {1, 2, 3});
    ClientHttpResponse originalResponse = mock(ClientHttpResponse.class);
    when(originalResponse.getHeaders()).thenReturn(new HttpHeaders());
    when(originalResponse.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));
    when(originalResponse.getStatusText()).thenReturn("OK");
    when(originalResponse.getBody()).thenReturn(body);

    ClientHttpRequest originalRequest = mock(ClientHttpRequest.class);
    when(originalRequest.getURI()).thenReturn(java.net.URI.create("http://example.invalid/wms"));
    when(originalRequest.execute()).thenReturn(originalResponse);

    // When
    ClientHttpRequest cachedRequest = fetcher.register(originalRequest);
    ClientHttpResponse result = cachedRequest.execute();

    // Then: caching worked, and there was no need to force-close the original response (the
    // caching code closes the body stream itself as part of the normal, successful path).
    assertEquals(HttpStatusCode.valueOf(200), result.getStatusCode());
    verify(originalResponse, never()).close();
    result.close();
  }
}
