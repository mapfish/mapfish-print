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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.output.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test building processor graphs.
 * <p/>
 * @author jesseeichar on 3/24/14.
 */
public class ProcessorDependencyGraphFactoryTest extends AbstractMapfishSpringTest {
    private static final String EXECUTION_TRACKER = "executionOrder";

    @Autowired
    private ProcessorDependencyGraphFactory processorDependencyGraphFactory;

    static class TestOrderExecution {
        List<TestProcessor> testOrderExecution = new ArrayList<TestProcessor>();

        public void doExecute(TestProcessor p) {
            testOrderExecution.add(p);
        }
    }

    private ForkJoinPool forkJoinPool;

    @Before
    public void setUp() throws Exception {
        forkJoinPool = new ForkJoinPool(1);
    }

    @After
    public void tearDown() throws Exception {
        forkJoinPool.shutdownNow();
    }

    @Test
    public void testSimpleBuild() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(RootNoOutput, NeedsTable, NeedsMap, RootMapOut,
                RootTableAndWidthOut);
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootMapOut, RootNoOutput, RootTableAndWidthOut);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);
        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(execution.testOrderExecution.toString(), 5, execution.testOrderExecution.size());
        assertHasOrdering(execution, RootMapOut, NeedsMap);
        assertHasOrdering(execution, RootTableAndWidthOut, NeedsTable);
    }

    @Test
    public void testBuildProcessInputObject() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(RootOutputExecutionTracker, RootNoOutput, NeedsTable, NeedsMap,
                RootMapOut, RootTableAndWidthOut);
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootOutputExecutionTracker);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);

        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(0, execution.testOrderExecution.size());

        TestOrderExecution correctTracker = values.getObject(EXECUTION_TRACKER, TestOrderExecution.class);

        assertEquals(correctTracker.testOrderExecution.toString(), 7, correctTracker.testOrderExecution.size());
        assertEquals(RootOutputExecutionTracker, correctTracker.testOrderExecution.get(0));
        assertEquals(RootOutputExecutionTracker, correctTracker.testOrderExecution.get(1));

        assertHasOrdering(correctTracker, RootOutputExecutionTracker, RootMapOut, NeedsMap);
        assertHasOrdering(correctTracker, RootOutputExecutionTracker, RootTableAndWidthOut, NeedsTable);
    }

    /**
     * This test checks that when there are 2 or more processors that produce the same output there is an exception thrown
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildDependencyAttachesToLastElementProducingTheValue() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(NeedsMap, RootMapOut,
                NeedsMapAndWidthOutputsMap, RootTableAndWidthOut);

        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootMapOut, RootTableAndWidthOut);
    }

    /**
     * This test addresses the case where the processors have the same input and output and therefore could be dependent on itself.
     */
    @Test
    public void testBuildWhenOutputsMapToAllOtherInputs() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(NeedsMapProducesMap, NeedsTableProducesTable);
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), NeedsMapProducesMap, NeedsTableProducesTable);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);
        values.put("table", "tableValue");
        values.put("map", "mapValue");

        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(2, execution.testOrderExecution.size());

        assertTrue(execution.testOrderExecution.containsAll(Arrays.asList(NeedsMapProducesMap, NeedsTableProducesTable)));
    }

    private void assertHasOrdering(TestOrderExecution execution, TestProcessor... processors) {
        final ArrayList<TestProcessor> processorList = Lists.newArrayList(processors);

        for (int i = 0; i < processorList.size(); i++) {
            final TestProcessor p = processorList.get(0);
            int actualIndex = execution.testOrderExecution.indexOf(p);
            for (TestProcessor p2 : processorList.subList(i + 1, processorList.size())) {
                assertTrue(actualIndex < execution.testOrderExecution.indexOf(p2));
            }
        }
    }


    private void assertContainsProcessors(List<ProcessorGraphNode> nodes, Processor... processors) {
        final List<Object> actualProcessorsList = Lists.transform(nodes, new Function<ProcessorGraphNode, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable ProcessorGraphNode input) {
                return input.getProcessor();
            }
        });

        final String comparison = actualProcessorsList + " expected " + Arrays.asList(processors);

        assertEquals(comparison, processors.length, nodes.size());
        assertTrue(comparison, actualProcessorsList.containsAll(Arrays.asList(processors)));
    }

    private abstract static class TestProcessor implements Processor {
        public String name;

        @Override
        public final Map<String, Object> execute(Map<String, Object> values) throws Exception {
            TestOrderExecution tracker = (TestOrderExecution) values.get(EXECUTION_TRACKER);
            if (tracker != null) {
                tracker.doExecute(this);
            }
            values.putAll(getExtras());
            return values;
        }

        protected Map<? extends String, ?> getExtras() {
            return Collections.emptyMap();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static TestProcessor RootNoOutput = new TestProcessor() {
        {
            this.name = "RootNoOutput";
        }

        @Override
        public Map<String, String> getInputMapper() {
            return Collections.singletonMap(EXECUTION_TRACKER, EXECUTION_TRACKER);
        }

        @Override
        public Map<String, String> getOutputMapper() {
            return null;
        }
    };

    private static TestProcessor RootMapOut = new TestProcessor() {
        {
            this.name = "RootMapOut";
        }

        @Override
        public Map<String, String> getInputMapper() {
            return Collections.singletonMap(EXECUTION_TRACKER, EXECUTION_TRACKER);
        }

        @Override
        protected Map<? extends String, ?> getExtras() {
            return Collections.singletonMap("map", "map");
        }

        @Override
        public Map<String, String> getOutputMapper() {
            return Collections.singletonMap("map", "map");
        }
    };

    private static TestProcessor RootTableAndWidthOut = new TestProcessor() {
        {
            this.name = "RootTableAndWidthOut";
        }

        @Override
        public Map<String, String> getInputMapper() {
            return Collections.singletonMap(EXECUTION_TRACKER, EXECUTION_TRACKER);
        }

        @Override
        protected Map<? extends String, ?> getExtras() {
            final HashMap<String, Object> map = Maps.newHashMap();
            map.put("table", "tableData");
            map.put("width", 1);
            return map;
        }

        @Override
        public Map<String, String> getOutputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put("table", "table");
            map.put("width", "width");
            return map;
        }
    };

    private static TestProcessor NeedsMapAndWidthOutputsMap = new TestProcessor() {
        {
            this.name = "NeedsMapAndWidthOutputsMap";
        }

        @Override
        public Map<String, String> getInputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put(EXECUTION_TRACKER, EXECUTION_TRACKER);
            map.put("map", "map");
            map.put("width", "width");
            return map;
        }

        @Override
        protected Map<? extends String, ?> getExtras() {
            return Collections.singletonMap("map", "map");
        }

        @Override
        public Map<String, String> getOutputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put("map", "map");
            return map;
        }
    };

    private static TestProcessor NeedsMap = new TestProcessor() {
        {
            this.name = "NeedsMap";
        }

        @Override
        public Map<String, String> getInputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put(EXECUTION_TRACKER, EXECUTION_TRACKER);
            map.put("map", "map");
            return map;
        }

        @Override
        public Map<String, String> getOutputMapper() {
            return null;
        }
    };

    private static TestProcessor NeedsTable = new TestProcessor() {
        {
            this.name = "NeedsTable";
        }

        @Override
        public Map<String, String> getInputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put("table", "table");
            map.put(EXECUTION_TRACKER, EXECUTION_TRACKER);
            return map;
        }

        @Override
        public Map<String, String> getOutputMapper() {
            return null;
        }
    };

    private static TestProcessor RootOutputExecutionTracker = new TestProcessor() {
        {
            this.name = "RootOutputExecutionTracker";
        }

        @Override
        public Map<String, String> getInputMapper() {
            return null;
        }

        @Override
        protected Map<? extends String, ?> getExtras() {
            final TestOrderExecution value = new TestOrderExecution();
            value.testOrderExecution.add(this);
            value.testOrderExecution.add(this);
            return Collections.singletonMap(EXECUTION_TRACKER, value);
        }

        @Override
        public Map<String, String> getOutputMapper() {
            return Collections.singletonMap(EXECUTION_TRACKER, EXECUTION_TRACKER);
        }
    };

    private static TestProcessor NeedsMapProducesMap = new TestProcessor() {
        {
            name = "NeedsMapProducesMap";
        }
        @Nullable
        @Override
        public Map<String, String> getInputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put("map", "map");
            map.put(EXECUTION_TRACKER, EXECUTION_TRACKER);
            return map;
        }

        @Nullable
        @Override
        public Map<String, String> getOutputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put("map", "map");
            return map;
        }
    };

    private static TestProcessor NeedsTableProducesTable = new TestProcessor() {
        {
            name = "NeedsTableProducesTable";
        }
        @Nullable
        @Override
        public Map<String, String> getInputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put("table", "table");
            map.put(EXECUTION_TRACKER, EXECUTION_TRACKER);
            return map;
        }

        @Nullable
        @Override
        public Map<String, String> getOutputMapper() {
            final HashMap<String, String> map = Maps.newHashMap();
            map.put("table", "table");
            return map;
        }
    };
}
