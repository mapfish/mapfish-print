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

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.output.Values;

import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author jesseeichar on 3/25/14.
 */
@SuppressWarnings("unchecked")
public class ProcessorDependencyGraphTest {
    @Test
    public void testToString() throws Exception {
        ProcessorDependencyGraph graph = new ProcessorDependencyGraph();
        ProcessorGraphNode root1 = new ProcessorGraphNode(new TestProcessor("root1"), null);
        ProcessorGraphNode root2 = new ProcessorGraphNode(new TestProcessor("root2"), null);
        ProcessorGraphNode dep11 = new ProcessorGraphNode(new TestProcessor("dep11"), null);
        ProcessorGraphNode dep21 = new ProcessorGraphNode(new TestProcessor("dep21"), null);
        ProcessorGraphNode dep11_1 = new ProcessorGraphNode(new TestProcessor("dep11_1"), null);
        ProcessorGraphNode dep11_2 = new ProcessorGraphNode(new TestProcessor("dep11_2"), null);
        graph.addRoot(root1);
        graph.addRoot(root2);

        root1.addDependency(dep11);
        root2.addDependency(dep21);

        dep11.addDependency(dep11_1);
        dep11.addDependency(dep11_2);
        assertEquals("dep11_1", dep11_1.toString());
        assertEquals("dep11\n  +-- dep11_1\n  +-- dep11_2", dep11.toString());
        assertEquals("+ root1\n  +-- dep11\n    +-- dep11_1\n    +-- dep11_2\n+ root2\n  +-- dep21", graph.toString());
    }


    @Test
    public void testCreateTaskAllDependenciesAreSatisfied() throws Exception {
        Values values = new Values();
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
    public void testCreateTaskAllDependenciesAreMissing() throws Exception {
        Values values = new Values();
        // this is a misconfiguration prop should be pp thus an exception should be thrown below.
        values.put("prop", "value");

        final TestProcessor processor = new TestProcessor("p");
        processor.getInputMapperBiMap().put("pp", "prop");

        final ProcessorDependencyGraph graph = new ProcessorDependencyGraph();
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
        public Void execute(TestIn values, ExecutionContext context) throws Exception {
            assertNotNull(values);
            assertNotNull(values.prop);
            assertNotNull(values.values);

            return null;
        }
    }
}
