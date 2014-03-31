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
import jsr166y.ForkJoinPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.output.Values;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        final ArrayList<Processor> processors = Lists.newArrayList(RootOutputExecutionTracker, RootNoOutput, NeedsTable, NeedsMap,
                RootMapOut, RootTableAndWidthOut);
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootOutputExecutionTracker);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);

        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(0, execution.testOrderExecution.size());

        TestOrderExecution correctTracker = values.getObject(EXECUTION_TRACKER, TestOrderExecution.class);

        assertEquals(correctTracker.testOrderExecution.toString(), 5, correctTracker.testOrderExecution.size());

        assertHasOrdering(correctTracker, RootMapOut, NeedsMap);
        assertHasOrdering(correctTracker, RootTableAndWidthOut, NeedsTable);
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


    /**
     * This test checks that all the outputMapper mappings have an associated property in the output object.
     *
     * @throws Exception
     */
    @Test(expected = RuntimeException.class)
    public void testExtraOutputMapperMapping() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(NeedsMap, RootMapOut,
                NeedsMapAndWidthOutputsMap, RootTableAndWidthOut);

        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootMapOut, RootTableAndWidthOut);
    }

    private void assertHasOrdering(TestOrderExecution execution, Processor... processors) {
        final ArrayList<Processor> processorList = Lists.newArrayList(processors);

        for (int i = 0; i < processorList.size(); i++) {
            final Processor p = processorList.get(0);
            int actualIndex = execution.testOrderExecution.indexOf(p);
            for (Processor p2 : processorList.subList(i + 1, processorList.size())) {
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

    static class TrackerContainer {
        public TestOrderExecution executionOrder;
    }
    private abstract static class TestProcessor<In extends TrackerContainer, Out>
            extends AbstractProcessor<In, Out> {
        public String name;

        protected TestProcessor(String name, Class<Out> outputType) {
            super(outputType);
            this.name = name;
        }

        @Override
        public final Out execute(In values) throws Exception {
            TestOrderExecution tracker = values.executionOrder;
            if (tracker != null) {
                tracker.doExecute(this);
            }
            return getExtras();
        }

        protected abstract Out getExtras();

        @SuppressWarnings("unchecked")
        @Override
        public In createInputParameter() {
            return (In) new TrackerContainer();
        }
        @Override
        public String toString() {
            return name;
        }
    }

    private static TestProcessor RootNoOutput = new TestProcessor<TrackerContainer, Void>("RootNoOutput", Void.class) {

        @Override
        protected Void getExtras() {
            return null;
        }
    };
    private static class MapOutput {
        public String map = "map";

    }
    private static TestProcessor RootMapOut = new TestProcessor<TrackerContainer, MapOutput>("RootMapOut", MapOutput.class) {

        @Override
        protected MapOutput getExtras() {
            return new MapOutput();
        }

    };
    private static class TableAndWidth {

        public String table = "tableData";
        public int width = 1;
    }
    private static TestProcessor RootTableAndWidthOut = new TestProcessor<TrackerContainer, TableAndWidth>("RootTableAndWidthOut",
            TableAndWidth.class) {

        @Override
        protected TableAndWidth getExtras() {
            return new TableAndWidth();
        }
    };
    static class MapInput extends TrackerContainer {
        public String map = "map";
    }

    private static class MapAndWidth extends MapInput {
        public int width;
    }
    private static TestProcessor NeedsMapAndWidthOutputsMap = new TestProcessor<MapAndWidth, MapOutput>("NeedsMapAndWidthOutputsMap",
            MapOutput.class) {
        @Override
        public MapAndWidth createInputParameter() {
            return new MapAndWidth();
        }

        @Override
        protected MapOutput getExtras() {
            return new MapOutput();
        }
    };

    private static TestProcessor NeedsMap = new TestProcessor<MapInput, Void>("NeedsMap", Void.class) {
        @Override
        protected Void getExtras() {
            return null;
        }

        @Override
        public MapInput createInputParameter() {
            return new MapInput();
        }
    };

    static class TableInput extends TrackerContainer {
        public String table;
    }
    private static TestProcessor NeedsTable = new TestProcessor<TableInput, Void>("NeedsTable", Void.class) {

        @Override
        protected Void getExtras() {
            return null;
        }

        @Override
        public TableInput createInputParameter() {
            return new TableInput();
        }
    };

    private static Processor RootOutputExecutionTracker = new AbstractProcessor<Void, TrackerContainer>(TrackerContainer.class) {
        @Override
        public Void createInputParameter() {
            return null;
        }

        @Nullable
        @Override
        public TrackerContainer execute(Void values) throws Exception {
            assertNull(values);
            final TrackerContainer trackerContainer = new TrackerContainer();
            trackerContainer.executionOrder = new TestOrderExecution();
            return trackerContainer;
        }

        @Override
        public String toString() {
            return "RootOutputExecutionTracker";
        }
    };

    private static TestProcessor NeedsMapProducesMap = new TestProcessor<MapInput, MapOutput>("NeedsMapProducesMap",
            MapOutput.class) {
        @Override
        public MapInput createInputParameter() {
            return new MapInput();
        }

        @Override
        protected MapOutput getExtras() {
            return new MapOutput();
        }
    };
    private static class TableOutput {
        public String table = "table";

    }
    private static TestProcessor NeedsTableProducesTable = new TestProcessor<TableInput, TableOutput>("NeedsTableProducesTable",
            TableOutput.class) {

        @Override
        protected TableOutput getExtras() {
            return new TableOutput();
        }

        @Override
        public TableInput createInputParameter() {
            return new TableInput();
        }
    };
}
