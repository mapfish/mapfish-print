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
import org.json.JSONException;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorGraphNode;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

public abstract class AbstractHttpProcessorTest extends AbstractMapfishSpringTest {
    @Autowired
    ConfigurationFactory configurationFactory;
    @Autowired
    TestHttpClientFactory httpClientFactory;

    @Autowired
    ForkJoinPool forkJoinPool;

    protected abstract String baseDir();
    protected abstract Class<? extends AbstractTestProcessor> testProcessorClass();
    protected abstract Class<? extends HttpProcessor> classUnderTest();

    @Test
    @DirtiesContext
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
        final Configuration config = configurationFactory.getConfig(getFile(baseDir() + "/config.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(1, roots.size());
        final ProcessorGraphNode processor = roots.get(0);
        assertEquals(classUnderTest(), processor.getProcessor().getClass());

        final Set dependencies = processor.getAllProcessors();
        dependencies.remove(processor.getProcessor());
        assertEquals(1, dependencies.size());
        assertEquals(testProcessorClass(), dependencies.iterator().next().getClass());

        Values values = new Values();
        values.put(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, this.httpClientFactory);
        addExtraValues(values);
        forkJoinPool.invoke(graph.createTask(values));
    }

    protected void addExtraValues(Values values) throws JSONException {
        // default does nothing
    }

    @Test
    public void testCreateMapDependency() throws Exception {

        this.configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(baseDir() + "/config-createmap.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(1, roots.size());
        final ProcessorGraphNode compositeClientHttpRequestFactoryProcessor = roots.get(0);
        assertEquals(classUnderTest(), compositeClientHttpRequestFactoryProcessor.getProcessor().getClass());
        final Set dependencies = compositeClientHttpRequestFactoryProcessor.getAllProcessors();
        dependencies.remove(compositeClientHttpRequestFactoryProcessor.getProcessor());
        assertEquals(1, dependencies.size());
        assertEquals(CreateMapProcessor.class, dependencies.iterator().next().getClass());
    }

    public static class TestParam {
        public MfClientHttpRequestFactory clientHttpRequestFactory;
    }

    public static abstract class AbstractTestProcessor  extends AbstractProcessor<TestParam, Void> {

        /**
         * Constructor.
         */
        protected AbstractTestProcessor() {
            super(Void.class);
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
            // do nothing
        }

        @Nullable
        @Override
        public TestParam createInputParameter() {
            return new TestParam();
        }

    }
}
