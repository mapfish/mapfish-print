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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mapfish.print.config.ConfigurationFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.junit.Assert.assertEquals;

public class MfClientHttpRequestFactoryImpl_ResponseTest {
    private static final int TARGET_PORT = 33212;
    private static HttpServer targetServer;

    @Autowired
    ConfigurationFactory configurationFactory;
    @Autowired
    private MfClientHttpRequestFactoryImpl requestFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        targetServer = HttpServer.create(new InetSocketAddress(HttpProxyTest.LOCALHOST, TARGET_PORT), 0);
        targetServer.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        targetServer.stop(0);
    }

    @Test
    public void testGetHeaders() throws Exception {
        targetServer.createContext("/request", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                final Headers responseHeaders = httpExchange.getResponseHeaders();
                responseHeaders.add("Content-Type", "application/json; charset=utf8");
                httpExchange.sendResponseHeaders(200, 0);
                httpExchange.close();
            }
        });


        MfClientHttpRequestFactoryImpl factory = new MfClientHttpRequestFactoryImpl();
        final ConfigurableRequest request = factory.createRequest(
                new URI("http://" + HttpProxyTest.LOCALHOST + ":" + TARGET_PORT + "/request"), HttpMethod.GET);

        final ClientHttpResponse response = request.execute();
        assertEquals("application/json; charset=utf8", response.getHeaders().getFirst("Content-Type"));
    }
}