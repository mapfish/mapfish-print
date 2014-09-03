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

import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.NoSuchElementException;

/**
 * This request factory will attempt to load resources using {@link org.mapfish.print.config.Configuration#loadFile(String)}
 * and {@link org.mapfish.print.config.Configuration#isAccessible(String)} to load the resources if the http method is GET and
 * will fallback to the normal/wrapped factory to make http requests.
 *
 * @author Jesse on 8/12/2014.
 */
public final class ConfigFileResolvingHttpRequestFactory extends AbstractClientHttpRequestFactoryWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileResolvingHttpRequestFactory.class);
    private final Configuration config;

    /**
     * Constructor.
     *
     * @param httpRequestFactory basic request factory
     * @param config the template for the current print job.
     */
    public ConfigFileResolvingHttpRequestFactory(final ClientHttpRequestFactory httpRequestFactory,
                                                 final Configuration config) {
        super(httpRequestFactory);

        this.config = config;
    }

    @Override
    protected ClientHttpRequest createRequest(final URI uri,
                                              final HttpMethod httpMethod,
                                              final ClientHttpRequestFactory requestFactory) throws IOException {
        return new ConfigFileResolvingRequest(uri, httpMethod, requestFactory);
    }

    private class ConfigFileResolvingRequest extends AbstractClientHttpRequest {
        private final URI uri;
        private final HttpMethod httpMethod;
        private final ClientHttpRequestFactory requestFactory;
        private ClientHttpRequest request;


        ConfigFileResolvingRequest(final URI uri,
                                   final HttpMethod httpMethod,
                                   final ClientHttpRequestFactory requestFactory) {
            this.uri = uri;
            this.httpMethod = httpMethod;
            this.requestFactory = requestFactory;
        }

        @Override
        protected OutputStream getBodyInternal(final HttpHeaders headers) throws IOException {
            Assert.isTrue(this.request == null, "getBodyInternal() can only be called once.");
            this.request = createRequestFromWrapped(headers);
            return this.request.getBody();
        }

        private ClientHttpRequest createRequestFromWrapped(final HttpHeaders headers) throws IOException {
            ClientHttpRequest httpRequest = this.requestFactory.createRequest(this.uri, this.httpMethod);
            httpRequest.getHeaders().putAll(headers);
            return httpRequest;
        }

        @Override
        protected ClientHttpResponse executeInternal(final HttpHeaders headers) throws IOException {
            if (this.request != null) {
                LOGGER.debug("Executing http request: " + this.request.getURI());
                return this.request.execute();
            }
            if (this.httpMethod == HttpMethod.GET) {
                final String uriString = this.uri.toString();
                final Configuration configuration = ConfigFileResolvingHttpRequestFactory.this.config;
                try {
                    final byte[] bytes = configuration.loadFile(uriString);
                    final ConfigFileResolverHttpResponse response = new ConfigFileResolverHttpResponse(bytes, headers);
                    LOGGER.debug("Resolved request: " + uriString + " using mapfish print config file loaders.");
                    return response;
                } catch (NoSuchElementException e) {
                  // cannot be loaded by configuration so try http
                }
            }

            LOGGER.debug("Executing http request: " + this.getURI());
            return createRequestFromWrapped(headers).execute();
        }

        @Override
        public HttpMethod getMethod() {
            return this.httpMethod;
        }

        @Override
        public URI getURI() {
            return this.uri;
        }

        private class ConfigFileResolverHttpResponse implements ClientHttpResponse {
            private final HttpHeaders headers;
            private final byte[] bytes;

            ConfigFileResolverHttpResponse(final byte[] bytes,
                                           final HttpHeaders headers) {
                this.headers = headers;
                this.bytes = bytes;
            }

            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return getStatusCode().value();
            }

            @Override
            public String getStatusText() throws IOException {
                return "OK";
            }

            @Override
            public void close() {
                // nothing to do
            }

            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(this.bytes);
            }

            @Override
            public HttpHeaders getHeaders() {
                return this.headers;
            }
        }
    }
}
