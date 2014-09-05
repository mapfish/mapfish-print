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

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;

/**
 * @author Jesse on 9/3/2014.
 */
public abstract class AbstractMfClientHttpRequestFactoryWrapper implements MfClientHttpRequestFactory {

    private final MfClientHttpRequestFactory wrappedFactory;


    /**
     * Creates a {@code AbstractClientHttpRequestFactoryWrapper} wrapping the given request factory.
     * @param wrappedFactory the request factory to be wrapped
     */
    protected AbstractMfClientHttpRequestFactoryWrapper(final MfClientHttpRequestFactory wrappedFactory) {
        Assert.notNull(wrappedFactory, "'requestFactory' must not be null");
        this.wrappedFactory = wrappedFactory;
    }


    /**
     * This implementation simply calls {@link #createRequest(URI, HttpMethod, MfClientHttpRequestFactory)}
     * with the wrapped request factory provided to the
     * {@linkplain #AbstractMfClientHttpRequestFactoryWrapper(MfClientHttpRequestFactory) constructor}.
     *
     * @param uri the URI to create a request for
     * @param httpMethod the HTTP method to execute
     */
    public final ClientHttpRequest createRequest(final URI uri,
                                                 final HttpMethod httpMethod) throws IOException {
        return createRequest(uri, httpMethod, this.wrappedFactory);
    }

    /**
     * Create a new {@link ClientHttpRequest} for the specified URI and HTTP method by using the
     * passed-on request factory.
     * <p>Called from {@link #createRequest(URI, HttpMethod)}.
     *
     * @param uri the URI to create a request for
     * @param httpMethod the HTTP method to execute
     * @param requestFactory the wrapped request factory
     *
     * @return the created request
     * @throws IOException in case of I/O errors
     */
    protected abstract ClientHttpRequest createRequest(final URI uri,
                                                       final HttpMethod httpMethod,
                                                       final MfClientHttpRequestFactory requestFactory) throws IOException;

    @Override
    public final void register(final RequestConfigurator callback) {
        this.wrappedFactory.register(callback);
    }
}
