package org.mapfish.print.processor;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.output.Values;

import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class ProcessorDependencyGraphTest {
    @Test
    public void testToString() {
        ProcessorDependencyGraph graph = new ProcessorDependencyGraph();
        ProcessorGraphNode root1 = new ProcessorGraphNode(new TestProcessor("root1"), new MetricRegistry());
        ProcessorGraphNode root2 = new ProcessorGraphNode(new TestProcessor("root2"), new MetricRegistry());
        ProcessorGraphNode dep11 = new ProcessorGraphNode(new TestProcessor("dep11"), new MetricRegistry());
        ProcessorGraphNode dep21 = new ProcessorGraphNode(new TestProcessor("dep21"), new MetricRegistry());
        ProcessorGraphNode dep11_1 =
                new ProcessorGraphNode(new TestProcessor("dep11_1"), new MetricRegistry());
        ProcessorGraphNode dep11_2 =
                new ProcessorGraphNode(new TestProcessor("dep11_2"), new MetricRegistry());
        graph.addRoot(root1);
        graph.addRoot(root2);

        root1.addDependency(dep11);
        root2.addDependency(dep21);

        dep11.addDependency(dep11_1);
        dep11.addDependency(dep11_2);
        assertEquals("\"?\" -> \"dep11_1\";\n", dep11_1.toString());
        assertTrue(dep11.toString(),
                   "\"?\" -> \"dep11\";\n  \"dep11\" -> \"dep11_1\";\n  \"dep11\" -> \"dep11_2\";\n"
                           .equals(dep11.toString()) ||
                           "\"?\" -> \"dep11\";\n  \"dep11\" -> \"dep11_2\";\n  \"dep11\" -> \"dep11_1\";\n"
                                   .equals(dep11.toString()));
        assertEquals("\"?\" -> \"root2\";\n  \"root2\" -> \"dep21\";\n", root2.toString());
    }


    @Test
    public void testCreateTaskAllDependenciesAreSatisfied() {
        Values values = new Values();
        values.put(Values.VALUES_KEY, values);
        values.put("pp", "value");
        final TestProcessor processor = new TestProcessor("p");
        processor.getInputMapperBiMap().put("pp", "prop");

        final ProcessorDependencyGraph graph = new ProcessorDependencyGraph();
        MetricRegistry registry = new MetricRegistry();
        graph.addRoot(new ProcessorGraphNode(processor, registry));
        final ProcessorDependencyGraph.ProcessorGraphForkJoinTask task = graph.createTask(values);

        // no exception ... good

        task.compute();

        // no exceptions? good.
        // processor execute method has the assertion checks and is called by compute

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateTaskAllDependenciesAreMissing() {
        Values values = new Values();
        // this is a misconfiguration prop should be pp thus an exception should be thrown below.
        values.put("prop", "value");
        values.put(Values.JOB_ID_KEY, "test");

        final TestProcessor processor = new TestProcessor("p");
        processor.getInputMapperBiMap().put("pp", "prop");

        final ProcessorDependencyGraph graph = new ProcessorDependencyGraph();
        //noinspection ConstantConditions
        graph.addRoot(new ProcessorGraphNode(processor, null));
        graph.createTask(values);
    }

    static class TestIn {
        public String prop;
        public Values values;
    }

    private static class TestProcessor extends AbstractProcessor<TestIn, Void> {
        private final String name;

        protected TestProcessor(String name) {
            super(Void.class);
            this.name = name;
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
            // no checks
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public TestIn createInputParameter() {
            return new TestIn();
        }

        @Nullable
        @Override
        public Void execute(TestIn values, ExecutionContext context) {
            assertNotNull(values);
            assertNotNull(values.prop);
            assertNotNull(values.values);

            return null;
        }
    }
}
