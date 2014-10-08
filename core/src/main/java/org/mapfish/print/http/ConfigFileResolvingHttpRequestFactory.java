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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;

/**
 * This request factory will attempt to load resources using {@link org.mapfish.print.config.Configuration#loadFile(String)}
 * and {@link org.mapfish.print.config.Configuration#isAccessible(String)} to load the resources if the http method is GET and
 * will fallback to the normal/wrapped factory to make http requests.
 *
 * @author Jesse on 8/12/2014.
 */
public final class ConfigFileResolvingHttpRequestFactory implements MfClientHttpRequestFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileResolvingHttpRequestFactory.class);
    private final Configuration config;
    private final MfClientHttpRequestFactoryImpl httpRequestFactory;
    private final List<RequestConfigurator> callbacks = Lists.newCopyOnWriteArrayList();

    /**
     * Constructor.
     *
     * @param httpRequestFactory basic request factory
     * @param config             the template for the current print job.
     */
    public ConfigFileResolvingHttpRequestFactory(final MfClientHttpRequestFactoryImpl httpRequestFactory,
                                                 final Configuration config) {
        this.httpRequestFactory = httpRequestFactory;
        this.config = config;
    }

    @Override
    public void register(@Nonnull final RequestConfigurator callback) {
        this.callbacks.add(callback);
    }

    @Override
    public ClientHttpRequest createRequest(final URI uri,
                                           final HttpMethod httpMethod) throws IOException {
        return new ConfigFileResolvingRequest(uri, httpMethod);
    }


    private class ConfigFileResolvingRequest extends AbstractClientHttpRequest {
        private final URI uri;
        private final HttpMethod httpMethod;
        private ConfigurableRequest request;


        ConfigFileResolvingRequest(final URI uri,
                                   final HttpMethod httpMethod) {
            this.uri = uri;
            this.httpMethod = httpMethod;
        }

        @Override
        protected synchronized OutputStream getBodyInternal(final HttpHeaders headers) throws IOException {
            Assert.isTrue(this.request == null, "getBodyInternal() can only be called once.");
            this.request = createRequestFromWrapped(headers);
            return this.request.getBody();
        }

        private synchronized ConfigurableRequest createRequestFromWrapped(final HttpHeaders headers) throws IOException {
            final MfClientHttpRequestFactoryImpl requestFactory = ConfigFileResolvingHttpRequestFactory.this.httpRequestFactory;
            ConfigurableRequest httpRequest = requestFactory.createRequest(this.uri, this.httpMethod);
            httpRequest.setConfiguration(ConfigFileResolvingHttpRequestFactory.this.config);

            httpRequest.getHeaders().putAll(headers);
            return httpRequest;
        }

        @Override
        protected synchronized ClientHttpResponse executeInternal(final HttpHeaders headers) throws IOException {
            if (this.request != null) {
                LOGGER.debug("Executing http request: " + this.request.getURI());
                return executeCallbacksAndRequest(this.request);
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
            return executeCallbacksAndRequest(createRequestFromWrapped(headers));
        }

        private ClientHttpResponse executeCallbacksAndRequest(final ConfigurableRequest requestToExecute) throws IOException {
            for (RequestConfigurator callback : ConfigFileResolvingHttpRequestFactory.this.callbacks) {
                callback.configureRequest(requestToExecute);
            }

            return requestToExecute.execute();
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
