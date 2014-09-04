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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.http.MapfishClientHttpRequestFactoryImpl;
import org.mapfish.print.processor.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        AbstractMapfishSpringTest.DEFAULT_SPRING_XML
})
public class HttpProxyProcessorTest {
    private static HttpServer server;

    @Autowired
    private MapfishClientHttpRequestFactoryImpl requestFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        InetSocketAddress address = new InetSocketAddress("localhost", 23434);
        server = HttpServer.create(address, 0);
        server.createContext("/request", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                httpExchange.sendResponseHeaders(200, 0);
            }
        });

        server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop(0);
    }

    @Test
    public void testExecute() throws Exception {
        final HttpProxyProcessor httpProxyProcessor = new HttpProxyProcessor();

        ClientHttpFactoryProcessorParam values = new ClientHttpFactoryProcessorParam();
        Configuration config = new Configuration();
        final File configFile = AbstractMapfishSpringTest.getFile(HttpProxyProcessorTest.class, "/org/mapfish/print/servlet/config.yaml");
        config.setConfigurationFile(configFile);
        values.clientHttpRequestFactory = new ConfigFileResolvingHttpRequestFactory(requestFactory, config);
        Processor.ExecutionContext context = new Processor.ExecutionContext() {
            @Override
            public boolean isCanceled() {
                return false;
            }
        };
        final ClientHttpFactoryProcessorParam output = httpProxyProcessor.execute(values, context);

        URI uri = new URI("http://localhost:43221/request");
        final ClientHttpRequest request = output.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
        final ClientHttpResponse response = request.execute();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private URI createServerURI() throws URISyntaxException {
        return new URI("http://" + server.getAddress().getHostName() + ":" + server.getAddress().getPort());
    }
}