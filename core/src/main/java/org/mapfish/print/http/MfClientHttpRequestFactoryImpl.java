/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.http;

import com.google.common.io.Closer;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
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
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Default implementation.
 *
 * @author Jesse on 9/3/2014.
 */
public class MfClientHttpRequestFactoryImpl extends HttpComponentsClientHttpRequestFactory {
    private static final ThreadLocal<Configuration> CURRENT_CONFIGURATION = new InheritableThreadLocal<Configuration>();

    @Nullable
    static Configuration getCurrentConfiguration() {
        return CURRENT_CONFIGURATION.get();
    }

    /**
     * Constructor.
     */
    public MfClientHttpRequestFactoryImpl() {
        super(createHttpClient());
    }

    private static CloseableHttpClient createHttpClient() {
        final HttpClientBuilder httpClientBuilder = HttpClients.custom().
                setRoutePlanner(new MfRoutePlanner()).
                setSSLSocketFactory(new MfSSLSocketFactory()).
                setDefaultCredentialsProvider(new MfCredentialsProvider());
        return httpClientBuilder.build();
    }

    // CSOFF: DesignForExtension
    // allow extension only for testing
    @Override
    public ConfigurableRequest createRequest(@Nonnull final URI uri,
                                             @Nonnull final HttpMethod httpMethod) throws IOException {
        // CSON: DesignForExtension
        HttpRequestBase httpRequest = (HttpRequestBase) createHttpUriRequest(httpMethod, uri);
        return new Request(getHttpClient(), httpRequest, createHttpContext(httpMethod, uri));
    }

    /**
     * A request that can be configured at a low level.
     * <p/>
     * It is an http components based request.
     */
    public static final class Request extends AbstractClientHttpRequest implements ConfigurableRequest {

        private final HttpClient client;
        private final HttpRequestBase request;
        private final HttpContext context;
        private final ByteArrayOutputStream outputStream;
        private Configuration configuration;

        Request(@Nonnull final HttpClient client,
                @Nonnull final HttpRequestBase request,
                @Nonnull final HttpContext context) {
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

        public URI getURI() {
            return this.request.getURI();
        }

        @Override
        protected OutputStream getBodyInternal(@Nonnull final HttpHeaders headers) throws IOException {
            return this.outputStream;
        }

        @Override
        protected Response executeInternal(@Nonnull final HttpHeaders headers) throws IOException {
            CURRENT_CONFIGURATION.set(this.configuration);

            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                if (!headerName.equalsIgnoreCase(HTTP.CONTENT_LEN) &&
                    !headerName.equalsIgnoreCase(HTTP.TRANSFER_ENCODING)) {
                    for (String headerValue : entry.getValue()) {
                        this.request.addHeader(headerName, headerValue);
                    }
                }
            }
            if (this.request instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.request;
                Closer closer = Closer.create();
                try {
                    HttpEntity requestEntity = new ByteArrayEntity(this.outputStream.toByteArray());
                    entityEnclosingRequest.setEntity(requestEntity);
                } finally {
                    closer.close();
                }
            }
            HttpResponse response = this.client.execute(this.request, this.context);
            return new Response(response);
        }
    }

    static class Response extends AbstractClientHttpResponse {
        private static final Logger LOGGER = LoggerFactory.getLogger(Response.class);
        private static final AtomicInteger ID_COUNTER = new AtomicInteger();
        private final HttpResponse response;
        private final int id = ID_COUNTER.incrementAndGet();
        private Closer closer = Closer.create();
        private InputStream inputStream;


        public Response(@Nonnull final HttpResponse response) {
            this.response = response;
            LOGGER.trace("Creating Http Response object: " + this.id);
        }


        @Override
        public int getRawStatusCode() throws IOException {
            return this.response.getStatusLine().getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return this.response.getStatusLine().getReasonPhrase();
        }

        @Override
        protected void finalize() throws Throwable {
            close();
        }

        @Override
        public void close() {
            try {
                getBody();
            } catch (IOException e) {
                LOGGER.error("Error occurred while trying to retrieve Http Response " + this.id + " in order to close it.", e);
            } finally {
                try {
                    this.closer.close();
                } catch (IOException e) {
                    LOGGER.trace("Error while closing Http Response object: " + this.id);
                    throw new RuntimeException(e);
                }

                LOGGER.trace("Closed Http Response object: " + this.id);
            }
        }

        @Override
        public synchronized InputStream getBody() throws IOException {
            if (this.inputStream == null) {
                final HttpEntity entity = this.response.getEntity();
                if (entity != null) {
                    this.inputStream = this.closer.register(entity.getContent());
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
            for (Header header : allHeaders) {
                for (HeaderElement element : header.getElements()) {
                    translatedHeaders.add(header.getName(), element.toString());
                }
            }
            return translatedHeaders;
        }
    }
}
