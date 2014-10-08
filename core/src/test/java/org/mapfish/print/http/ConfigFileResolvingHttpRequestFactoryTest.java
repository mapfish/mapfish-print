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

import com.google.common.base.Predicate;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.IllegalFileAccessException;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.net.URI;

import static org.junit.Assert.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigFileResolvingHttpRequestFactoryTest extends AbstractMapfishSpringTest {

    private static final String BASE_DIR = "/org/mapfish/print/servlet/";
    private static final String HOST = "host.com";

    final File logbackXml = getFile("/logback.xml");
    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;

    private ConfigFileResolvingHttpRequestFactory resolvingFactory;

    @Before
    public void setUp() throws Exception {
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return true;
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile(uri.getPath()));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
        );

        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

        this.resolvingFactory = new ConfigFileResolvingHttpRequestFactory(this.requestFactory, config);
    }

    @Test
    public void testCreateRequestServlet() throws Exception {
        final String path = BASE_DIR + "requestData.json";
        final URI uri = new URI("servlet://" + path);
        final ClientHttpRequest request =
                resolvingFactory.createRequest(uri, HttpMethod.GET);

        final ClientHttpResponse response = request.execute();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String expected = Files.toString(getFile(path), Constants.DEFAULT_CHARSET);
        final String actual = new String(ByteStreams.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateRequestHttpGet() throws Exception {
        final URI uri = new URI("http://" + HOST + ".test/logback.xml");
        final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);
        final ClientHttpResponse response = request.execute();
        final String actual = new String(ByteStreams.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
        String expected = Files.toString(logbackXml, Constants.DEFAULT_CHARSET);
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateRequestHttpPost() throws Exception {
        URI uri = new URI("http://" + HOST + ".test/logback.xml");
        ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.POST);
        ClientHttpResponse response = request.execute();
        String actual = new String(ByteStreams.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
        String expected = Files.toString(logbackXml, Constants.DEFAULT_CHARSET);
        assertEquals(expected, actual);

        uri = logbackXml.toURI();
        request = resolvingFactory.createRequest(uri, HttpMethod.POST);
        response = request.execute();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    }

    @Test
    public void testCreateRequestHttpWriteToBody() throws Exception {

        URI uri = new URI("http://" + HOST + ".test/logback.xml");
        ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);
        request.getBody().write(new byte[]{1,2,3});
        ClientHttpResponse response = request.execute();
        String actual = new String(ByteStreams.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
        String expected = Files.toString(logbackXml, Constants.DEFAULT_CHARSET);
        assertEquals(expected, actual);

        uri = logbackXml.toURI();
        request = resolvingFactory.createRequest(uri, HttpMethod.GET);
        request.getBody().write(new byte[]{1,2,3});
        response = request.execute();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testCreateRequestFile() throws Exception {
        final String path = BASE_DIR + "requestData.json";
        final URI uri = getFile(path).toURI();
        final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);

        final ClientHttpResponse response = request.execute();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String expected = Files.toString(getFile(path), Constants.DEFAULT_CHARSET);
        final String actual = new String(ByteStreams.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateRequestRelativeFileToConfig() throws Exception {
        final String path = BASE_DIR + "requestData.json";
        final URI uri = new URI("file://requestData.json");
        final ClientHttpRequest request =
                resolvingFactory.createRequest(uri, HttpMethod.GET);

        final ClientHttpResponse response = request.execute();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String expected = Files.toString(getFile(path), Constants.DEFAULT_CHARSET);
        final String actual = new String(ByteStreams.toByteArray(response.getBody()), Constants.DEFAULT_CHARSET);
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalFileAccessException.class)
    public void testCreateRequestIllegalFile() throws Exception {
        final URI uri = logbackXml.toURI();
        final ClientHttpRequest request = resolvingFactory.createRequest(uri, HttpMethod.GET);

        request.execute();
    }
}