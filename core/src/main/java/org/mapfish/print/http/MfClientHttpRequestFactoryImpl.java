package org.mapfish.print.http;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation.
 */
public class MfClientHttpRequestFactoryImpl extends HttpComponentsClientHttpRequestFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MfClientHttpRequestFactoryImpl.class);
    private static final ThreadLocal<Configuration> CURRENT_CONFIGURATION = new InheritableThreadLocal<>();

    /**
     * Constructor.
     *
     * @param maxConnTotal Maximum total connections.
     * @param maxConnPerRoute Maximum connections per route.
     */
    public MfClientHttpRequestFactoryImpl(final int maxConnTotal, final int maxConnPerRoute) {
        super(createHttpClient(maxConnTotal, maxConnPerRoute));
    }

    @Nullable
    static Configuration getCurrentConfiguration() {
        return CURRENT_CONFIGURATION.get();
    }

    private static int getIntProperty(final String name) {
        final String value = System.getProperty(name);
        if (value == null) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    private static CloseableHttpClient createHttpClient(final int maxConnTotal, final int maxConnPerRoute) {
        final RequestConfig requestConfig =
                RequestConfig.custom().
                        setConnectionRequestTimeout(getIntProperty("http.connectionRequestTimeout")).
                        setConnectTimeout(getIntProperty("http.connectTimeout")).
                        setSocketTimeout(getIntProperty("http.socketTimeout")).build();

        final HttpClientBuilder httpClientBuilder = HttpClients.custom().
                disableCookieManagement().
                setDnsResolver(new RandomizingDnsResolver()).
                setRoutePlanner(new MfRoutePlanner()).
                setSSLSocketFactory(new MfSSLSocketFactory()).
                setDefaultCredentialsProvider(new MfCredentialsProvider()).
                setDefaultRequestConfig(requestConfig).
                setMaxConnTotal(maxConnTotal).
                setMaxConnPerRoute(maxConnPerRoute);
        return httpClientBuilder.build();
    }

    // allow extension only for testing
    @Override
    public ConfigurableRequest createRequest(
            @Nonnull final URI uri,
            @Nonnull final HttpMethod httpMethod) throws IOException {
        HttpRequestBase httpRequest = (HttpRequestBase) createHttpUriRequest(httpMethod, uri);
        return new Request(getHttpClient(), httpRequest, createHttpContext(httpMethod, uri));
    }

    /**
     * Randomized order DnsResolver.
     * <p>
     * The default DnsResolver is using the results of InetAddress.getAllByName which is cached and returns
     * the IP addresses always in the same order (think about DNS round robin). The callers always try the
     * addresses in the order returned by the DnsResolver. This implementation adds randomizing to it's
     * result.
     */
    private static final class RandomizingDnsResolver extends SystemDefaultDnsResolver {
        @Override
        public InetAddress[] resolve(final String host) throws UnknownHostException {
            final List<InetAddress> list = Arrays.asList(super.resolve(host));
            Collections.shuffle(list);
            return list.toArray(new InetAddress[list.size()]);
        }
    }

    /**
     * A request that can be configured at a low level.
     *
     * It is an http components based request.
     */
    public static final class Request extends AbstractClientHttpRequest implements ConfigurableRequest {

        private final HttpClient client;
        private final HttpRequestBase request;
        private final HttpContext context;
        private final ByteArrayOutputStream outputStream;
        private Configuration configuration;

        Request(
                @Nonnull final HttpClient client,
                @Nonnull final HttpRequestBase request,
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

        public HttpRequestBase getUnderlyingRequest() {
            return this.request;
        }

        public HttpMethod getMethod() {
            return HttpMethod.valueOf(this.request.getMethod());
        }

        @Override
        public String getMethodValue() {
            return this.request.getMethod();
        }

        public URI getURI() {
            return this.request.getURI();
        }

        @Override
        protected OutputStream getBodyInternal(@Nonnull final HttpHeaders headers) {
            return this.outputStream;
        }

        @Override
        protected Response executeInternal(@Nonnull final HttpHeaders headers) throws IOException {
            CURRENT_CONFIGURATION.set(this.configuration);

            LOGGER.debug(
                "Preparing request {} {}: {}", this.getMethod(), this.getURI(),
                String.join("\n", Utils.getPrintableHeadersList(headers))
            );

            for (Map.Entry<String, List<String>> entry: headers.entrySet()) {
                String headerName = entry.getKey();
                if (!headerName.equalsIgnoreCase(HTTP.CONTENT_LEN) &&
                        !headerName.equalsIgnoreCase(HTTP.TRANSFER_ENCODING)) {
                    for (String headerValue: entry.getValue()) {
                        this.request.addHeader(headerName, headerValue);
                    }
                }
            }
            if (this.request instanceof HttpEntityEnclosingRequest) {
                final HttpEntityEnclosingRequest entityEnclosingRequest =
                        (HttpEntityEnclosingRequest) this.request;
                final HttpEntity requestEntity = new ByteArrayEntity(this.outputStream.toByteArray());
                entityEnclosingRequest.setEntity(requestEntity);
            }
            HttpResponse response = this.client.execute(this.request, this.context);
            LOGGER.debug("Response: {} -- {}", response.getStatusLine().getStatusCode(), this.getURI());

            return new Response(response);
        }
    }

    static class Response extends AbstractClientHttpResponse {
        private static final Logger LOGGER = LoggerFactory.getLogger(Response.class);
        private static final AtomicInteger ID_COUNTER = new AtomicInteger();
        private final HttpResponse response;
        private final int id = ID_COUNTER.incrementAndGet();
        private InputStream inputStream;


        Response(@Nonnull final HttpResponse response) {
            this.response = response;
            LOGGER.trace("Creating Http Response object: {}", this.id);
        }


        @Override
        public int getRawStatusCode() {
            return this.response.getStatusLine().getStatusCode();
        }

        @Override
        public String getStatusText() {
            return this.response.getStatusLine().getReasonPhrase();
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            close();
        }

        @Override
        public void close() {
            try {
                getBody();
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("Error occurred while trying to retrieve Http Response {} in order to close it.",
                             this.id, e);
            }
            LOGGER.trace("Closed Http Response object: {}", this.id);
        }

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

        @Override
        public HttpHeaders getHeaders() {
            final HttpHeaders translatedHeaders = new HttpHeaders();
            final Header[] allHeaders = this.response.getAllHeaders();
            for (Header header: allHeaders) {
                for (HeaderElement element: header.getElements()) {
                    translatedHeaders.add(header.getName(), element.toString());
                }
            }
            return translatedHeaders;
        }
    }
}
