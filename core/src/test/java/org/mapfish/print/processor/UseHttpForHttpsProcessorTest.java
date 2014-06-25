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

package org.mapfish.print.processor;

import com.google.common.base.Predicate;
import jsr166y.ForkJoinPool;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/use-http-for-https/add-custom-processor-application-context.xml"
})
public class UseHttpForHttpsProcessorTest extends AbstractMapfishSpringTest {

    private static final String BASE_DIR = "use-http-for-https";
    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    TestHttpClientFactory httpClientFactory;

    @Autowired
    private ForkJoinPool forkJoinPool;

    @Test
    public void testExecute() throws Exception {
        this.httpClientFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return true;
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                return new MockClientHttpRequest(httpMethod, uri);
            }
        });

        this.configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "/config.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(1, roots.size());
        assertEquals(UseHttpForHttpsProcessor.class, roots.get(0).getProcessor().getClass());

        Values values = new Values();
        values.put(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, this.httpClientFactory);
        forkJoinPool.invoke(graph.createTask(values));
    }

    @Test
    public void testCreateMapDependency() throws Exception {

        this.configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "/config-createmap.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(1, roots.size());
        final ProcessorGraphNode useHttpForHttpsProcessorNode = roots.get(0);
        assertEquals(UseHttpForHttpsProcessor.class, useHttpForHttpsProcessorNode.getProcessor().getClass());
        final Set dependencies = useHttpForHttpsProcessorNode.getAllProcessors();
        dependencies.remove(useHttpForHttpsProcessorNode.getProcessor());
        assertEquals(1, dependencies.size());
        assertEquals(CreateMapProcessor.class, dependencies.iterator().next().getClass());
    }

    public static class TestParam {
        public ClientHttpRequestFactory clientHttpRequestFactory;
    }

    public static class TestProcessor extends AbstractProcessor<TestParam, Void> {

        /**
         * Constructor.
         */
        protected TestProcessor() {
            super(Void.class);
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors) {
            // do nothing
        }

        @Nullable
        @Override
        public TestParam createInputParameter() {
            return new TestParam();
        }

        String userinfo = "user:pass";
        String host = "localhost";
        String path = "path";
        String query = "query";
        String fragment = "fragment";

        @Nullable
        @Override
        public Void execute(TestParam values, ExecutionContext context) throws Exception {
            testDefinedPortMapping(values);
            testImplicitPortMapping(values);
            testUriWithOnlyAuthoritySegment(values);
            testHttp(values);
            return null;
        }

        private void testUriWithOnlyAuthoritySegment(TestParam values) throws URISyntaxException, IOException {
            String authHost = "center_wmts_fixedscale.com";

            URI uri = new URI("https://" + userinfo + "@" + authHost + ":8443/" + path);
            ClientHttpRequest request = values.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            assertEquals("http", request.getURI().getScheme());
            assertEquals(userinfo + "@" + authHost + ":9999", request.getURI().getAuthority());
            assertEquals("/" + path, request.getURI().getPath());


            uri = new URI("https://" + authHost + ":8443/" + path);
            request = values.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            assertEquals("http", request.getURI().getScheme());
            assertEquals(authHost + ":9999", request.getURI().getAuthority());

            uri = new URI("https://" + authHost + "/" + path);
            request = values.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            assertEquals("http", request.getURI().getScheme());
            assertEquals(authHost, request.getURI().getAuthority());

            uri = new URI("https://" + userinfo + "@" + authHost + "/" + path);
            request = values.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            assertEquals("http", request.getURI().getScheme());
            assertEquals(userinfo + "@" + authHost, request.getURI().getAuthority());
        }

        private void testHttp(TestParam values) throws URISyntaxException, IOException {
            String uriString = String.format("http://%s@%s:9999/%s?%s#%s", userinfo, host, path, query, fragment);
            final ClientHttpRequest request = values.clientHttpRequestFactory.createRequest(new URI(uriString), HttpMethod.GET);
            assertEquals("http", request.getURI().getScheme());
            assertEquals(userinfo, request.getURI().getUserInfo());
            assertEquals(host, request.getURI().getHost());
            assertEquals(9999, request.getURI().getPort());
            assertEquals("/" + path, request.getURI().getPath());
            assertEquals(query, request.getURI().getQuery());
            assertEquals(fragment, request.getURI().getFragment());
        }

        private void testDefinedPortMapping(TestParam values) throws IOException, URISyntaxException {
            String uriString = String.format("https://%s@%s:8443/%s?%s#%s", userinfo, host, path, query, fragment);
            final ClientHttpRequest request = values.clientHttpRequestFactory.createRequest(new URI(uriString), HttpMethod.GET);
            assertEquals("http", request.getURI().getScheme());
            assertEquals(userinfo, request.getURI().getUserInfo());
            assertEquals(host, request.getURI().getHost());
            assertEquals(9999, request.getURI().getPort());
            assertEquals("/" + path, request.getURI().getPath());
            assertEquals(query, request.getURI().getQuery());
            assertEquals(fragment, request.getURI().getFragment());
        }

        private void testImplicitPortMapping(TestParam values) throws IOException, URISyntaxException {
            String uriString = String.format("https://%s@%s/%s?%s#%s", userinfo, host, path, query, fragment);
            final ClientHttpRequest request = values.clientHttpRequestFactory.createRequest(new URI(uriString), HttpMethod.GET);
            assertEquals("http", request.getURI().getScheme());
            assertEquals(userinfo, request.getURI().getUserInfo());
            assertEquals(host, request.getURI().getHost());
            assertEquals(-1, request.getURI().getPort());
            assertEquals("/" + path, request.getURI().getPath());
            assertEquals(query, request.getURI().getQuery());
            assertEquals(fragment, request.getURI().getFragment());
        }
    }
}