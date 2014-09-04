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

package org.mapfish.print.processor.http;

import org.mapfish.print.http.AbstractMfClientHttpRequestFactoryWrapper;
import org.mapfish.print.http.MapfishClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * This processor configures the requests to use a proxy for making all http requests.
 *
 * <p>Example - Explicitly declare that requests to localhost or www.camptocamp.org require proxying: </p>
 * <pre><code>
 * - !proxy
 *   requireProxy:
 *     - !localMatch {}
 *     - !ipMatch
 *       ip: www.camptocamp.org
 *   host: proxy.host.com
 *   port: 8888
 *   username: username
 *   password: xyzpassword
 * </code></pre>
 * <p>Example - Proxy all requests except localhost and www.camptocamp.org: </p>
 * <pre><code>
 * - !proxy
 *   noProxy:
 *     - !localMatch {}
 *     - !ipMatch
 *       ip: www.camptocamp.org
 *   host: proxy.host.com
 *   port: 8888
 *   username: username
 *   password: xyzpassword
 * </code></pre>
 *
 * @author Jesse on 6/25/2014.
 */
public final class HttpProxyProcessor extends AbstractClientHttpRequestFactoryProcessor {
    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {

    }

    @Override
    public MapfishClientHttpRequestFactory createFactoryWrapper(final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
                                                         final MapfishClientHttpRequestFactory requestFactory) {

        return new AbstractMfClientHttpRequestFactoryWrapper(requestFactory) {
            @Override
            protected ClientHttpRequest createRequest(final URI uri,
                                                      final HttpMethod httpMethod,
                                                      final MapfishClientHttpRequestFactory requestFactory) throws IOException {
                final ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
                return request;
            }
        };
    }
}
