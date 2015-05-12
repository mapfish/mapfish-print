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

import com.google.common.base.Predicate;
import jsr166y.ForkJoinPool;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorGraphNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/http/map-uri/map-uri-228-bug-fix-processor-application-context.xml"
})
public class MapUriBug228ProcessorTest extends AbstractMapfishSpringTest {
    @Autowired
    ConfigurationFactory configurationFactory;
    @Autowired
    TestHttpClientFactory httpClientFactory;
    @Autowired
    ForkJoinPool forkJoinPool;


    protected String baseDir() {
        return "map-uri";
    }
    @Test
    public void bug228FixMapUri() throws Exception {
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
        final Configuration config = configurationFactory.getConfig(getFile(baseDir() + "/map-uri-228-bug-fix-config.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(1, roots.size());

        Values values = new Values();
        values.put(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, this.httpClientFactory);
        forkJoinPool.invoke(graph.createTask(values));
    }

    public static class TestProcessor extends AbstractProcessor<AbstractHttpProcessorTest.TestParam, Void> {
        /**
         * Constructor.
         */
        protected TestProcessor() {
            super(Void.class);
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
            // do nothing
        }

        @Nullable
        @Override
        public AbstractHttpProcessorTest.TestParam createInputParameter() {
            return new AbstractHttpProcessorTest.TestParam();
        }

        @Nullable
        @Override
        public Void execute(AbstractHttpProcessorTest.TestParam values, ExecutionContext context) throws Exception {
            final URI uri = new URI("http://geomapfish.demo-camptocamp.com/path?query#fragment");
            final ClientHttpRequest request = values.clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            final URI finalUri = request.getURI();

            assertEquals("http", finalUri.getScheme());
            assertEquals("127.0.0.1", finalUri.getHost());
            assertEquals("/path", finalUri.getPath());
            assertEquals(-1, finalUri.getPort());
            assertEquals("query", finalUri.getQuery());
            assertEquals("fragment", finalUri.getFragment());

            return null;
        }
    }
}