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
import com.google.common.collect.Sets;
import com.vividsolutions.jts.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mapfish.print.json.parser.JsonParserUtils.getAllAttributes;

/**
 * Class for constructing {@link org.mapfish.print.processor.ProcessorDependencyGraph} instances.
 * <p/>
 *
 * @author jesseeichar on 3/24/14.
 */
public final class ProcessorDependencyGraphFactory {

    @Autowired
    private MetricRegistry metricRegistry;


    /**
     * Create a {@link ProcessorDependencyGraph}.
     *
     * @param processors the processors that will be part of the graph
     * @return a {@link org.mapfish.print.processor.ProcessorDependencyGraph} constructed from the passed in processors
     */
    @SuppressWarnings("unchecked")
    public ProcessorDependencyGraph build(final List<? extends Processor> processors) {
        ProcessorDependencyGraph graph = new ProcessorDependencyGraph();

        final Map<String, ProcessorGraphNode> provideBy = new HashMap<String, ProcessorGraphNode>();
        final List<ProcessorGraphNode> nodes = new ArrayList<ProcessorGraphNode>(processors.size());

        for (Processor<Object, Object> processor : processors) {

            final ProcessorGraphNode<Object, Object> node = new ProcessorGraphNode<Object, Object>(processor, this.metricRegistry);
            for (String value : getOutputValues(node)) {
                if (provideBy.containsKey(value)) {
                    throw new IllegalArgumentException("Multiple processors provide the same output mapping: '" + processor + "' and '" +
                                                       provideBy.get(value) + "' both provide: '" + value +
                                                       "'.  You have to rename one of the outputs and the corresponding input so that" +
                                                       " there is no ambiguity with regards to the input a processor consumes.");
                }

                provideBy.put(value, node);
            }
            nodes.add(node);
        }

        for (ProcessorGraphNode<Object, Object> node : nodes) {
            final Set<String> inputs = getInputs(node);
            if (inputs.isEmpty()) {
                graph.addRoot(node);
            } else {
                boolean isDependency = false;
                for (String requiredKey : inputs) {
                    final ProcessorGraphNode solution = provideBy.get(requiredKey);
                    if (solution != null && solution != node) {
                        isDependency = true;
                        solution.addDependency(node);
                    }
                }

                if (!isDependency) {
                    graph.addRoot(node);
                }
            }
        }

        Assert.isTrue(graph.getAllProcessors().containsAll(processors), graph + "does not contain all the processors: " + processors);

        return graph;
    }

    private static Set<String> getInputs(final ProcessorGraphNode<Object, Object> node) {
        final Map<String, String> inputMapper = node.getInputMapper();
        final Set<String> inputs = Sets.newHashSet(inputMapper.keySet());

        final Object inputParameter = node.getProcessor().createInputParameter();
        if (inputParameter != null) {
            final Collection<Field> allProperties = getAllAttributes(inputParameter.getClass());
            for (Field descriptor : allProperties) {
                inputs.add(descriptor.getName());
            }
        }

        return inputs;
    }

    private static Collection<String> getOutputValues(final ProcessorGraphNode<Object, Object> node) {
        final Map<String, String> outputMapper = node.getOutputMapper();
        final Set<String> values = Sets.newHashSet(outputMapper.values());

        final Collection<Field> allProperties = getAllAttributes(node.getProcessor().getOutputType());
        for (Field field : allProperties) {
            values.add(field.getName());
        }

        return values;
    }

}
