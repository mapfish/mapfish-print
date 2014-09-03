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

package org.mapfish.print.map.style.json;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import org.geotools.styling.Style;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.mapfish.print.servlet.fileloader.ServletConfigFileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.net.URI;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MapfishJsonFileResolverTest extends AbstractMapfishSpringTest {
    final TestHttpClientFactory httpClient = new TestHttpClientFactory();

    @Autowired
    private MapfishJsonStyleParserPlugin parser;

    @Autowired
    private ConfigFileLoaderManager fileLoaderManager;
    @Autowired
    private ServletConfigFileLoader configFileLoader;

    @Test
    public void testLoadFromFile() throws Throwable {
        final String rootFile = getFile("/test-http-request-factory-application-context.xml").getParentFile().getAbsolutePath();
        configFileLoader.setServletContext(new MockServletContext(rootFile));

        final String configFile = "/org/mapfish/print/map/style/json/requestData-style-json-v1-style.json";
        final String styleString = "v2-style-symbolizers-default-values.json";
        final Optional<Style> styleOptional = loadStyle(configFile, styleString);
        assertTrue(styleOptional.isPresent());
        assertNotNull(styleOptional.get());
    }

    @Test
    public void testLoadFromServlet() throws Throwable {
        final File rootFile = getFile("/test-http-request-factory-application-context.xml").getParentFile();
        configFileLoader.setServletContext(new MockServletContext(new ResourceLoader() {
            @Override
            public Resource getResource(String location) {
                final File file = new File(rootFile, location);
                if (file.exists()) {
                    return new FileSystemResource(file);
                }
                throw new IllegalArgumentException(file + " not found");
            }

            @Override
            public ClassLoader getClassLoader() {
                return MapfishJsonFileResolverTest.class.getClassLoader();
            }
        }));

        final String configFile = "/org/mapfish/print/map/style/json/requestData-style-json-v1-style.json";
        final String styleString = "servlet:///org/mapfish/print/map/style/json/v2-style-symbolizers-default-values.json";
        final Optional<Style> styleOptional = loadStyle(configFile, styleString);

        assertTrue(styleOptional.isPresent());
        assertNotNull(styleOptional.get());
    }

    @Test
    @DirtiesContext
    public void testLoadFromURL() throws Throwable {
        final String rootFile = getFile("/test-http-request-factory-application-context.xml").getParentFile().getAbsolutePath();
        configFileLoader.setServletContext(new MockServletContext(rootFile));


        final String host = "URLSLDParserPluginTest.com";
        httpClient.registerHandler(new Predicate<URI>() {
                                                     @Override
                                                     public boolean apply(URI input) {
                                                         return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
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

        Configuration configuration = new Configuration();
        configuration.setFileLoaderManager(this.fileLoaderManager);
        final String path = "/org/mapfish/print/map/style/json/v2-style-symbolizers-default-values.json";
        configuration.setConfigurationFile(getFile(path));

        final Optional<Style> styleOptional = parser.parseStyle(configuration, this.httpClient,
                "http://URLSLDParserPluginTest.com" + path, null);

        assertTrue(styleOptional.isPresent());
        assertNotNull(styleOptional.get());
    }

    @Test
    public void testLoadFromClasspath() throws Throwable {

        final String rootFile = getFile("/test-http-request-factory-application-context.xml").getParentFile().getAbsolutePath();
        configFileLoader.setServletContext(new MockServletContext(rootFile));

        final String configFile = "/org/mapfish/print/map/style/json/v2-style-symbolizers-default-values.json";
        final String styleString = "classpath://" + configFile;
        final Optional<Style> styleOptional = loadStyle(configFile, styleString);

        assertTrue(styleOptional.isPresent());
        assertNotNull(styleOptional.get());
    }


    private Optional<Style> loadStyle(String configFile, String styleString) throws Throwable {
        Configuration configuration = new Configuration();
        configuration.setFileLoaderManager(this.fileLoaderManager);
        configuration.setConfigurationFile(getFile(configFile));

        ConfigFileResolvingHttpRequestFactory requestFactory = new ConfigFileResolvingHttpRequestFactory(this.httpClient, configuration);

        return parser.parseStyle(configuration, requestFactory,
                styleString, null);
    }
}
