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

package org.mapfish.print;


import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Allows tests to provide canned responses to requests.
 *
 * @author Jesse on 4/4/14.
 */
public class TestHttpClientFactory implements ClientHttpRequestFactory {

    private final Map<Predicate<URI>, Handler> handlers = Maps.newConcurrentMap();

    public void registerHandler(Predicate<URI> matcher, Handler handler) {
        if (handlers.containsKey(matcher)) {
            throw new IllegalArgumentException(matcher + " has already been registered");
        }
        handlers.put(matcher, handler);
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        for (Map.Entry<Predicate<URI>, Handler> entry : handlers.entrySet()) {
            if (entry.getKey().apply(uri)) {
                try {
                    return entry.getValue().handleRequest(uri, httpMethod);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalArgumentException(uri + " not registered with " + getClass().getName());
    }

    public static abstract class Handler {
        public abstract MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws IOException, Exception;
        public MockClientHttpRequest ok(URI uri, byte[] bytes, HttpMethod httpMethod) {
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
            ClientHttpResponse response = new MockClientHttpResponse(bytes, HttpStatus.OK);
            request.setResponse(response);
            return request;
        }

        public MockClientHttpRequest error404(URI uri, HttpMethod httpMethod) {
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
            MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.NOT_FOUND);
            request.setResponse(response);
            return request;
        }

        public MockClientHttpRequest failOnExecute(final URI uri, final HttpMethod httpMethod) {
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected ClientHttpResponse executeInternal() throws IOException {
                    fail("request should not be executed " + uri.toString());
                    throw new IOException();
                }
            };
            return request;
        }
    }
}
