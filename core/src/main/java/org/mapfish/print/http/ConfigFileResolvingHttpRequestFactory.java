package org.mapfish.print.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.annotation.Nonnull;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

/**
 * This request factory will attempt to load resources using {@link
 * org.mapfish.print.config.Configuration#loadFile(String)} and {@link
 * org.mapfish.print.config.Configuration#isAccessible(String)} to load the resources if the http
 * method is GET and will fall back to the normal/wrapped factory to make http requests.
 */
public final class ConfigFileResolvingHttpRequestFactory implements MfClientHttpRequestFactory {
  private final Configuration config;
  @Nonnull private final Map<String, String> mdcContext;
  private final MfClientHttpRequestFactoryImpl httpRequestFactory;
  private final List<RequestConfigurator> callbacks = new CopyOnWriteArrayList<>();

  /** Maximum number of attempts to try to fetch the same http request in case it is failing. */
  @Value("${httpRequest.fetchRetry.maxNumber}")
  private int httpRequestMaxNumberFetchRetry;

  @Value("${httpRequest.fetchRetry.intervalMillis}")
  private int httpRequestFetchRetryIntervalMillis;

  /**
   * Constructor.
   *
   * @param httpRequestFactory basic request factory
   * @param config the template for the current print job.
   * @param mdcContext the mdc context for the current print job.
   */
  public ConfigFileResolvingHttpRequestFactory(
      final MfClientHttpRequestFactoryImpl httpRequestFactory,
      final Configuration config,
      @Nonnull final Map<String, String> mdcContext,
      final int httpRequestMaxNumberFetchRetry,
      final int httpRequestFetchRetryIntervalMillis) {
    this.httpRequestFactory = httpRequestFactory;
    this.config = config;
    this.mdcContext = mdcContext;
    this.httpRequestMaxNumberFetchRetry = httpRequestMaxNumberFetchRetry;
    this.httpRequestFetchRetryIntervalMillis = httpRequestFetchRetryIntervalMillis;
  }

  @Override
  public void register(@Nonnull final RequestConfigurator callback) {
    this.callbacks.add(callback);
  }

  @Override
  @Nonnull
  public ClientHttpRequest createRequest(
      @Nonnull final URI uri, @Nonnull final HttpMethod httpMethod) {
    return new ConfigFileResolvingRequest(this, uri, httpMethod);
  }

  public MfClientHttpRequestFactoryImpl getHttpRequestFactory() {
    return httpRequestFactory;
  }

  @Nonnull
  public Map<String, String> getMdcContext() {
    return mdcContext;
  }

  public Configuration getConfig() {
    return config;
  }

  public int getHttpRequestMaxNumberFetchRetry() {
    return httpRequestMaxNumberFetchRetry;
  }

  public int getHttpRequestFetchRetryIntervalMillis() {
    return httpRequestFetchRetryIntervalMillis;
  }

  public List<RequestConfigurator> getCallbacks() {
    return callbacks;
  }
}
