package org.mapfish.print.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/** Default implementation. */
public class MfClientHttpRequestFactoryImpl extends HttpComponentsClientHttpRequestFactory {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MfClientHttpRequestFactoryImpl.class);
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
    final RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectionRequestTimeout(connectionRequestTimeout, TimeUnit.MILLISECONDS)
            .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .setResponseTimeout(socketTimeout, TimeUnit.MILLISECONDS)
            .build();

    final HttpClientBuilder httpClientBuilder =
      HttpClients.custom()
        .disableCookieManagement()
        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
          .setTlsSocketStrategy(new MfTLSSocketStrategy())
          .setDnsResolver(new RandomizingDnsResolver())
          .setMaxConnPerRoute(maxConnTotal)
          .setMaxConnPerRoute(maxConnPerRoute)
          .build())
        .setRoutePlanner(new MfRoutePlanner())
        .setDefaultCredentialsProvider(new MfCredentialsProvider())
        .setDefaultRequestConfig(requestConfig)
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
  private static final class RandomizingDnsResolver extends SystemDefaultDnsResolver {
    @Override
    public InetAddress[] resolve(final String host) throws UnknownHostException {
      final List<InetAddress> list = Arrays.asList(super.resolve(host));
      Collections.shuffle(list);
      return list.toArray(new InetAddress[0]);
    }
  }

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

    @Override
    public HttpMethod getMethod() {
      return HttpMethod.valueOf(this.request.getMethod());
    }

    @Nonnull
    public URI getURI() {
      try {
        return this.request.getUri();
      } catch (URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
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

      for (Map.Entry<String, List<String>> entry : headers.headerSet()) {
        String headerName = entry.getKey();
        if (!headerName.equalsIgnoreCase(org.apache.hc.core5.http.HttpHeaders.CONTENT_LENGTH)
            && !headerName.equalsIgnoreCase(org.apache.hc.core5.http.HttpHeaders.TRANSFER_ENCODING)) {
          for (String headerValue : entry.getValue()) {
            this.request.addHeader(headerName, headerValue);
          }
        }
      }
      if (this.request instanceof HttpEntityContainer entityEnclosingRequest) {
        var bytes = this.outputStream.toByteArray();
        final HttpEntity requestEntity = new ByteArrayEntity(bytes, ContentType.APPLICATION_OCTET_STREAM);
        entityEnclosingRequest.setEntity(requestEntity);
      }

      ClassicHttpResponse response = this.client.executeOpen(null, this.request, this.context);
      LOGGER.debug("Response: {} -- {}", response.getCode(), this.getURI());

      return new Response(response);
    }
  }

  // TODO: Do we need this class after all?
  public static class Response implements ClientHttpResponse {
    private static final Logger LOGGER = LoggerFactory.getLogger(Response.class);
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    private final ClassicHttpResponse response;
    private final int id = ID_COUNTER.incrementAndGet();
    private InputStream inputStream;

    Response(@Nonnull final ClassicHttpResponse response) {
      this.response = response;
      LOGGER.trace("Creating Http Response object: {}", this.id);
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
      return HttpStatusCode.valueOf(this.response.getCode());
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
