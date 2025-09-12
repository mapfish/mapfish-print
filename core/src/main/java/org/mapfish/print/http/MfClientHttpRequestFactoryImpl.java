package org.mapfish.print.http;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/** Default implementation. */
public class MfClientHttpRequestFactoryImpl extends HttpComponentsClientHttpRequestFactory {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MfClientHttpRequestFactoryImpl.class);
  // import org.apache.hc.client5.http.impl.DnsResolver; // Non utilisé
  private static final ThreadLocal<Configuration> CURRENT_CONFIGURATION =
      new InheritableThreadLocal<>();

  /**
   * Constructor.
   *
   * @param maxConnTotal Maximum total connections.
   * @param maxConnPerRoute Maximum connections per route.
   * @param connectionRequestTimeout Number of milliseconds used when requesting a connection from
   *     the connection manager.
   * @param connectTimeout Number of milliseconds until a connection is established.
   * @param socketTimeout Maximum number of milliseconds during which a socket remains inactive
   *     between two consecutive data packets.
   */
  public MfClientHttpRequestFactoryImpl(
      final int maxConnTotal,
      final int maxConnPerRoute,
      final int connectionRequestTimeout,
      final int connectTimeout,
      final int socketTimeout) {
    super(
        createHttpClient(
            maxConnTotal,
            maxConnPerRoute,
            connectionRequestTimeout,
            connectTimeout,
            socketTimeout));
  }

  @Nullable
  static Configuration getCurrentConfiguration() {
    return CURRENT_CONFIGURATION.get();
  }

  private static CloseableHttpClient createHttpClient(
      final int maxConnTotal,
      final int maxConnPerRoute,
      final int connectionRequestTimeout,
      final int connectTimeout,
      final int socketTimeout) {
    final org.apache.hc.client5.http.config.RequestConfig requestConfig =
        org.apache.hc.client5.http.config.RequestConfig.custom()
            .setConnectionRequestTimeout(
                org.apache.hc.core5.util.Timeout.ofMilliseconds(connectionRequestTimeout))
            .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(connectTimeout))
            .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofMilliseconds(socketTimeout))
            .build();

    final HttpClientBuilder httpClientBuilder =
        HttpClients.custom()
            .disableCookieManagement()
            // .setDnsResolver(new RandomizingDnsResolver()) // Adapter si besoin
            // .setRoutePlanner(new MfRoutePlanner()) // Adapter si besoin
            // .setSSLSocketFactory(new MfSSLSocketFactory()) // Adapter si besoin
            // .setDefaultCredentialsProvider(new MfCredentialsProvider()) // Adapter si besoin
            .setDefaultRequestConfig(requestConfig)
            .setMaxConnTotal(maxConnTotal)
            .setMaxConnPerRoute(maxConnPerRoute)
            .setUserAgent(UserAgentCreator.getUserAgent());
    CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
    LOGGER.debug(
        "Created CloseableHttpClient using connectionRequestTimeout: {} connectTimeout: {}"
            + " socketTimeout: {}",
        connectionRequestTimeout,
        connectTimeout,
        socketTimeout);
    return closeableHttpClient;
  }

  // allow extension only for testing
  @Override
  @Nonnull
  public ConfigurableRequest createRequest(
      @Nonnull final URI uri, @Nonnull final HttpMethod httpMethod) {
    HttpUriRequestBase httpRequest = (HttpUriRequestBase) createHttpUriRequest(httpMethod, uri);
    return new Request(getHttpClient(), httpRequest, createHttpContext(httpMethod, uri));
  }

  /**
   * Randomized order DnsResolver.
   *
   * <p>The default DnsResolver is using the results of InetAddress.getAllByName which is cached and
   * returns the IP addresses always in the same order (think about DNS round robin). The callers
   * always try the addresses in the order returned by the DnsResolver. This implementation adds
   * randomizing to it's result.
   */
  // TODO: Adapter RandomizingDnsResolver pour HttpClient 5 si besoin

  /**
   * A request that can be configured at a low level.
   *
   * <p>It is an http components based request.
   */
  public static final class Request extends AbstractClientHttpRequest
      implements ConfigurableRequest {

    private final HttpClient client;
    private final HttpUriRequestBase request;
    private final HttpContext context;
    private final ByteArrayOutputStream outputStream;
    private Configuration configuration;

    Request(
        @Nonnull final HttpClient client,
        @Nonnull final HttpUriRequestBase request,
        @Nullable final HttpContext context) {
      this.client = client;
      this.request = request;
      this.context = context;
      this.outputStream = new ByteArrayOutputStream();
    }

    public void setConfiguration(final Configuration configuration) {
      this.configuration = configuration;
    }

    public HttpClient getClient() {
      return this.client;
    }

    public HttpContext getContext() {
      return this.context;
    }

    public HttpUriRequestBase getUnderlyingRequest() {
      return this.request;
    }

    public HttpMethod getMethod() {
      return HttpMethod.valueOf(this.request.getMethod());
    }

    @Nonnull
    @Override
    public String getMethodValue() {
      return this.request.getMethod();
    }

    @Nonnull
    public URI getURI() {
      return this.request.getUri();
    }

    @Nonnull
    @Override
    protected OutputStream getBodyInternal(@Nonnull final HttpHeaders headers) {
      return this.outputStream;
    }

    @Nonnull
    @Override
    protected Response executeInternal(@Nonnull final HttpHeaders headers) throws IOException {
      CURRENT_CONFIGURATION.set(this.configuration);

      LOGGER.debug(
          "Preparing request {} {}: {}",
          this.getMethod(),
          this.getURI(),
          String.join("\n", Utils.getPrintableHeadersList(headers)));

      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String headerName = entry.getKey();
        if (!headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
            && !headerName.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
          for (String headerValue : entry.getValue()) {
            this.request.addHeader(headerName, headerValue);
          }
        }
      }
      // Pour POST, injecter l'entité si nécessaire
      if ("POST".equalsIgnoreCase(this.request.getMethod())) {
        final HttpEntity requestEntity = new ByteArrayEntity(this.outputStream.toByteArray());
        ((HttpPost) this.request).setEntity(requestEntity);
      }
      org.apache.hc.client5.http.classic.HttpResponse response =
          ((CloseableHttpClient) this.client).execute(this.request, this.context);
      LOGGER.debug("Response: {} -- {}", response.getCode(), this.getURI());
      return new Response(response);
    }
  }

  public static class Response extends AbstractClientHttpResponse {
    private static final Logger LOGGER = LoggerFactory.getLogger(Response.class);
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    private final org.apache.hc.client5.http.classic.HttpResponse response;
    private final int id = ID_COUNTER.incrementAndGet();
    private InputStream inputStream;

    Response(@Nonnull final org.apache.hc.client5.http.classic.HttpResponse response) {
      this.response = response;
      LOGGER.trace("Creating Http Response object: {}", this.id);
    }

    @Override
    public int getRawStatusCode() {
      return this.response.getCode();
    }

    @Nonnull
    @Override
    public String getStatusText() {
      return this.response.getReasonPhrase();
    }

    @Override
    public void close() {
      try {
        getBody();
        if (inputStream != null) {
          inputStream.close();
          inputStream = null;
        }
      } catch (IOException e) {
        LOGGER.error(
            "Error occurred while trying to retrieve Http Response {} in order to close it.",
            this.id,
            e);
        inputStream = null;
      }
      LOGGER.trace("Closed Http Response object: {}", this.id);
    }

    @Nonnull
    @Override
    public synchronized InputStream getBody() throws IOException {
      if (this.inputStream == null) {
        final HttpEntity entity = this.response.getEntity();
        if (entity != null) {
          this.inputStream = entity.getContent();
        }

        if (this.inputStream == null) {
          this.inputStream = new ByteArrayInputStream(new byte[0]);
        }
      }
      return this.inputStream;
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
      final HttpHeaders translatedHeaders = new HttpHeaders();
      final Header[] allHeaders = this.response.getHeaders();
      for (Header header : allHeaders) {
        translatedHeaders.add(header.getName(), header.getValue());
      }
      return translatedHeaders;
    }
  }
}
