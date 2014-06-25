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

import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/composite-client-http-request-factory/add-custom-processor-application-context.xml"
})
public class CompositeClientHttpRequestFactoryProcessorTest extends AbstractMapfishSpringTest {

    private static final String BASE_DIR = "composite-client-http-request-factory";
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
        final ProcessorGraphNode clientHttpRequestFactoryProcessor = roots.get(0);
        assertEquals(CompositeClientHttpRequestFactoryProcessor.class, clientHttpRequestFactoryProcessor.getProcessor().getClass());

        final Set dependencies = clientHttpRequestFactoryProcessor.getAllProcessors();
        dependencies.remove(clientHttpRequestFactoryProcessor.getProcessor());
        assertEquals(1, dependencies.size());
        assertEquals(TestProcessor.class, dependencies.iterator().next().getClass());

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
        final ProcessorGraphNode compositeClientHttpRequestFactoryProcessor = roots.get(0);
        assertEquals(CompositeClientHttpRequestFactoryProcessor.class, compositeClientHttpRequestFactoryProcessor.getProcessor().getClass());
        final Set dependencies = compositeClientHttpRequestFactoryProcessor.getAllProcessors();
        dependencies.remove(compositeClientHttpRequestFactoryProcessor.getProcessor());
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

        @Nullable
        @Override
        public Void execute(TestParam values, ExecutionContext context) throws Exception {
            final URI uri = new URI("https://localhost:8443/path?query#fragment");
            final ClientHttpRequest request = values.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            final URI finalUri = request.getURI();

            assertEquals("http", finalUri.getScheme());
            assertEquals("127.0.0.1", finalUri.getHost());
            assertEquals("/path", finalUri.getPath());
            assertEquals(9999, finalUri.getPort());
            assertEquals("query", finalUri.getQuery());
            assertEquals("fragment", finalUri.getFragment());

            return null;
        }
    }
}