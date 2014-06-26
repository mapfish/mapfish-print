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

import com.google.common.collect.Maps;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * This processor allows adding static headers to an http request.
 *
 * @author Jesse on 6/26/2014.
 */
public final class AddHeadersProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private Map<String, List<String>> headers = Maps.newHashMap();

    public void setHeaders(final Map<String, List<String>> headers) {
        this.headers = headers;
    }


    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.headers.isEmpty()) {
            validationErrors.add(new IllegalStateException("There are no headers defined."));
        }
    }

    @Override
    public ClientHttpRequestFactory createFactoryWrapper(final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
                                                         final ClientHttpRequestFactory requestFactory) {
        return new AbstractClientHttpRequestFactoryWrapper(requestFactory) {
            @Override
            protected ClientHttpRequest createRequest(final URI uri,
                                                      final HttpMethod httpMethod,
                                                      final ClientHttpRequestFactory requestFactory) throws
                    IOException {
                final ClientHttpRequest request = requestFactory.createRequest(uri, httpMethod);
                request.getHeaders().putAll(AddHeadersProcessor.this.headers);
                return request;
            }
        };

    }
}
