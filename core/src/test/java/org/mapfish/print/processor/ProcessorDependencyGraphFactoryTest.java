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
import com.google.common.collect.Sets;
import jsr166y.ForkJoinPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.map.CreateOverviewMapProcessor;
import org.mapfish.print.processor.map.SetStyleProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

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
    @DirtiesContext
    public void testSimpleBuild_ExternalDependency_NoInput() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(RootNoOutput, NeedsTable, NeedsMap, RootMapOut,
                RootTableAndWidthOut);
        
        // external dependency: RootNoOutput should run before RootMapOuts
        final List<ProcessorDependency> dependencies = Lists.newArrayList(
                new ProcessorDependency(RootNoOutputClass.class, RootMapOutClass.class));
        this.processorDependencyGraphFactory.setDependencies(dependencies);
        
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootNoOutput, RootTableAndWidthOut);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);
        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(execution.testOrderExecution.toString(), 5, execution.testOrderExecution.size());
        assertHasOrdering(execution, RootMapOut, NeedsMap);
        assertHasOrdering(execution, RootTableAndWidthOut, NeedsTable);
        assertHasOrdering(execution, RootNoOutput, RootMapOut);
    }
    
    @Test
    @DirtiesContext
    public void testSimpleBuild_ExternalDependency_SameInput() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(RootNoOutput, NeedsTable, NeedsMap, StyleNeedsMap,
                RootMapOut, RootTableAndWidthOut);
        
        // external dependency: StyleNeedsMap should run before NeedsMap when they have the same
        // mapped input for "map"
        final List<ProcessorDependency> dependencies = Lists.newArrayList(
                new ProcessorDependency(StyleNeedsMapClass.class, NeedsMapClass.class, Sets.newHashSet("map")));
        this.processorDependencyGraphFactory.setDependencies(dependencies);
        
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootNoOutput, RootTableAndWidthOut, RootMapOut);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);
        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(execution.testOrderExecution.toString(), 6, execution.testOrderExecution.size());
        assertHasOrdering(execution, RootMapOut, NeedsMap);
        assertHasOrdering(execution, RootTableAndWidthOut, NeedsTable);
        assertHasOrdering(execution, StyleNeedsMap, NeedsMap);
    }
    
    /**
     * This is a test for an external dependency on a common input property,
     * where the property has a different name for the two nodes.
     * For example, this is the case for {@link CreateOverviewMapProcessor} and
     * {@link SetStyleProcessor}. The map property for the overview map is called
     * `overviewMap` in the `CreateOverviewMapProcessor`, but `map` in the `SetStyleProcessor`.
     */
    @Test
    @DirtiesContext
    public void testSimpleBuild_ExternalDependency_SameInputWithDifferentName() throws Exception {
        NeedsOverviewMapAndMap.getInputMapperBiMap().put("map2", "overviewMap");
        TestProcessor styleNeedsMap2 = new StyleNeedsMapClass("StyleNeedsMap2", Void.class);
        styleNeedsMap2.getInputMapperBiMap().put("map2", "map");
        final ArrayList<TestProcessor> processors = Lists.newArrayList(RootNoOutput, NeedsTable, NeedsMap, StyleNeedsMap,
                RootMapOut, RootTableAndWidthOut, NeedsOverviewMapAndMap, styleNeedsMap2);
        
        // external dependency: StyleNeedsMap should run before NeedsMap when they have the same
        // mapped input for "map"
        final List<ProcessorDependency> dependencies = Lists.newArrayList(
                new ProcessorDependency(StyleNeedsMapClass.class, NeedsMapClass.class, Sets.newHashSet("map")));
        // external dependency: StyleNeedsMap should run before NeedsOverviewMap when they have the same
        // mapped input for "map" and "overviewMap"
        dependencies.add(
                new ProcessorDependency(StyleNeedsMapClass.class, NeedsOverviewMapAndMapClass.class,
                        Sets.newHashSet("map;overviewMap")));
        this.processorDependencyGraphFactory.setDependencies(dependencies);
        
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootNoOutput, RootTableAndWidthOut, RootMapOut, styleNeedsMap2);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);
        values.put("map2", "ov-map");
        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(execution.testOrderExecution.toString(), 8, execution.testOrderExecution.size());
        assertHasOrdering(execution, RootMapOut, NeedsMap);
        assertHasOrdering(execution, RootTableAndWidthOut, NeedsTable);
        assertHasOrdering(execution, StyleNeedsMap, NeedsMap);
        assertHasOrdering(execution, styleNeedsMap2, NeedsOverviewMapAndMap);
        
        // check that NeedsOverviewMap is not added as dependency to StyleNeedsMap
        ProcessorGraphNode<Object, Object> styleNode = getNodeForProcessor(graph.getRoots(), styleNeedsMap2);
        assertEquals(2, styleNode.getAllProcessors().size());
    }
    
    /**
     * This is a test for an external dependency with two maps.
     * It ensures that `NeedsMap2` only has a dependency on `StyleNeedsMap2` and
     * `NeedsMap` only a dependency on `StyleNeedsMap`. But not `NeedsMap2` on `StyleNeedsMap`
     * or `NeedsMap` on `StyleNeedsMap2`.
     */
    @Test
    @DirtiesContext
    public void testSimpleBuild_ExternalDependency_SameInputTwoMaps() throws Exception {
        // add processors for a second map
        TestProcessor needsMap2 = new NeedsMapClass("NeedsMap2", Void.class);
        needsMap2.getInputMapperBiMap().put("map2", "map");
        TestProcessor styleNeedsMap2 = new StyleNeedsMapClass("StyleNeedsMap2", Void.class);
        styleNeedsMap2.getInputMapperBiMap().put("map2", "map");

        final ArrayList<TestProcessor> processors = Lists.newArrayList(RootNoOutput, NeedsTable, NeedsMap, StyleNeedsMap,
                needsMap2, styleNeedsMap2, RootMapOut, RootTableAndWidthOut);
        
        // external dependency: StyleNeedsMap should run before NeedsMap when they have the same
        // mapped input for "map"
        final List<ProcessorDependency> dependencies = Lists.newArrayList(
                new ProcessorDependency(StyleNeedsMapClass.class, NeedsMapClass.class, Sets.newHashSet("map")));
        this.processorDependencyGraphFactory.setDependencies(dependencies);
        
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootNoOutput, RootTableAndWidthOut, RootMapOut, styleNeedsMap2);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);
        values.put("map2", "the 2nd map definition");
        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(execution.testOrderExecution.toString(), 8, execution.testOrderExecution.size());
        assertHasOrdering(execution, RootMapOut, NeedsMap);
        assertHasOrdering(execution, RootTableAndWidthOut, NeedsTable);
        assertHasOrdering(execution, StyleNeedsMap, NeedsMap);
        assertHasOrdering(execution, styleNeedsMap2, needsMap2);
        
        // check that NeedMap is not added as dependency to styleNeedsMap2
        ProcessorGraphNode<Object, Object> styleNode = getNodeForProcessor(graph.getRoots(), styleNeedsMap2);
        assertEquals(2, styleNode.getAllProcessors().size());
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

    @Test
    public void testBuildProcessInputHasValuesAndOtherInput() throws Exception {
        final NeedsValuesAndMap needsValuesAndMap = new NeedsValuesAndMap();
        final ArrayList<Processor> processors = Lists.<Processor>newArrayList(RootOutputExecutionTracker, needsValuesAndMap, RootMapOut);
        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootOutputExecutionTracker);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);

        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(0, execution.testOrderExecution.size());

        TestOrderExecution correctTracker = values.getObject(EXECUTION_TRACKER, TestOrderExecution.class);

        assertEquals(correctTracker.testOrderExecution.toString(), 2, correctTracker.testOrderExecution.size());

        assertHasOrdering(correctTracker, RootTableAndWidthOut, needsValuesAndMap);
    }

    @Test
    public void testBuildProcessInputHasValuesAndOtherInput_WithInputMapping() throws Exception {
        final NeedsValuesAndMap needsValuesAndMap = new NeedsValuesAndMap();
        final RootMapOutClass outMapProcessor = new RootMapOutClass("mapOut", MapOutput.class);
        outMapProcessor.getOutputMapperBiMap().put("map", "mapOutput");
        needsValuesAndMap.getInputMapperBiMap().put("mapOutput", "map");
        final ArrayList<Processor> processors = Lists.newArrayList(RootOutputExecutionTracker, needsValuesAndMap,
                outMapProcessor);

        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootOutputExecutionTracker);
        final TestOrderExecution execution = new TestOrderExecution();
        Values values = new Values();
        values.put(EXECUTION_TRACKER, execution);

        forkJoinPool.invoke(graph.createTask(values));

        assertEquals(0, execution.testOrderExecution.size());

        TestOrderExecution correctTracker = values.getObject(EXECUTION_TRACKER, TestOrderExecution.class);

        assertEquals(correctTracker.testOrderExecution.toString(), 2, correctTracker.testOrderExecution.size());

        assertHasOrdering(correctTracker, outMapProcessor, needsValuesAndMap);
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
    }

    /**
     * This test checks that when there are 2 or more processors that produce the same output, and this
     * output is annotated with @DebugOutput, that no exception is thrown.
     */
    @Test
    public void testBuildDebugOutput() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(RootDebugMapOut1, RootDebugMapOut2);

        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
        assertContainsProcessors(graph.getRoots(), RootDebugMapOut1, RootDebugMapOut2);
    }

    /**
     * Check that an exception is thrown when one processor provides an output value and
     * another processor expects an input value of the same name, but with a different type.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBuildValuesWithSameNameAndDifferentType() throws Exception {
        final ArrayList<TestProcessor> processors = Lists.newArrayList(NeedsMap, RootMapOut,
                NeedsMapList);

        ProcessorDependencyGraph graph = this.processorDependencyGraphFactory.build(processors);
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

    private ProcessorGraphNode<Object, Object> getNodeForProcessor(
            List<ProcessorGraphNode> roots, TestProcessor processor) {
        for (ProcessorGraphNode<Object, Object> node : roots) {
            if (node.getProcessor() == processor) {
                return node;
            }
        }
        return null;
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
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
            // no checks
        }

        @Override
        public final Out execute(In values, ExecutionContext context) throws Exception {
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

    private static class RootNoOutputClass extends TestProcessor<TrackerContainer, Void> {

        protected RootNoOutputClass(String name, Class<Void> outputType) {
            super(name, outputType);
        }

        @Override
        protected Void getExtras() {
            return null;
        }
    };
    
    private static TestProcessor RootNoOutput = new RootNoOutputClass("RootNoOutput", Void.class);
    
    private static class MapOutput {
        public String map = "map";

    }

    private static class RootMapOutClass extends TestProcessor<TrackerContainer, MapOutput> {
        
        protected RootMapOutClass(String name, Class<MapOutput> outputType) {
            super(name, outputType);
        }

        @Override
        protected MapOutput getExtras() {
            return new MapOutput();
        }
    }

    private static TestProcessor RootMapOut = new RootMapOutClass("RootMapOut", MapOutput.class);

    private static class DebugMapOutput {
        @InternalValue
        public String map = "map";
    }

    private static class RootDebugMapOutClass extends TestProcessor<TrackerContainer, DebugMapOutput> {

        protected RootDebugMapOutClass(String name, Class<DebugMapOutput> outputType) {
            super(name, outputType);
        }

        @Override
        protected DebugMapOutput getExtras() {
            return new DebugMapOutput();
        }
    }

    private static TestProcessor RootDebugMapOut1 = new RootDebugMapOutClass("RootDebugMapOut1", DebugMapOutput.class);
    private static TestProcessor RootDebugMapOut2 = new RootDebugMapOutClass("RootDebugMapOut2", DebugMapOutput.class);

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
    private static class MapInput extends TrackerContainer {
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

    private static class NeedsMapClass extends TestProcessor<MapInput, Void> {
        protected NeedsMapClass(String name, Class<Void> outputType) {
            super(name, outputType);
        }

        @Override
        protected Void getExtras() {
            return null;
        }

        @Override
        public MapInput createInputParameter() {
            return new MapInput();
        }
    }

    private static TestProcessor NeedsMap = new NeedsMapClass("NeedsMap", Void.class);

    private static class MapListInput extends TrackerContainer {
        public List<String> map = Lists.newArrayList();
    }

    private static class NeedsMapListClass extends TestProcessor<MapListInput, Void> {
        protected NeedsMapListClass(String name, Class<Void> outputType) {
            super(name, outputType);
        }

        @Override
        protected Void getExtras() {
            return null;
        }

        @Override
        public MapListInput createInputParameter() {
            return new MapListInput();
        }
    }

    private static TestProcessor NeedsMapList = new NeedsMapListClass("NeedsMapList", Void.class);

    private static class StyleNeedsMapClass extends TestProcessor<MapInput, Void> {
        protected StyleNeedsMapClass(String name, Class<Void> outputType) {
            super(name, outputType);
        }

        @Override
        protected Void getExtras() {
            return null;
        }

        @Override
        public MapInput createInputParameter() {
            return new MapInput();
        }
    }
    
    private static TestProcessor StyleNeedsMap = new StyleNeedsMapClass("StyleNeedsMap", Void.class);
    
    private static class OverviewMapInput extends TrackerContainer {
        public String overviewMap = "ov-map";
        public String map = "map";
    }

    private static class NeedsOverviewMapAndMapClass extends TestProcessor<OverviewMapInput, Void> {
        protected NeedsOverviewMapAndMapClass(String name, Class<Void> outputType) {
            super(name, outputType);
        }

        @Override
        protected Void getExtras() {
            return null;
        }

        @Override
        public OverviewMapInput createInputParameter() {
            return new OverviewMapInput();
        }
    }
    
    private static TestProcessor NeedsOverviewMapAndMap = new NeedsOverviewMapAndMapClass("NeedsOverviewMapAndMap", Void.class);

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
        public TrackerContainer execute(Void values, final ExecutionContext context) throws Exception {
            assertNull(values);
            final TrackerContainer trackerContainer = new TrackerContainer();
            trackerContainer.executionOrder = new TestOrderExecution();
            return trackerContainer;
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
            // no checks
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
    private static class MapValuesInput extends MapInput {
        @InternalValue
        public Values values;
    }
    private static class NeedsValuesAndMap extends TestProcessor<MapValuesInput, Void> {

        protected NeedsValuesAndMap() {
            super("NeedsTableProducesTable", Void.class);
        }

        @Override
        protected Void getExtras() {
            return null;
        }

        @Override
        public MapValuesInput createInputParameter() {
            return new MapValuesInput();
        }
    };
}
