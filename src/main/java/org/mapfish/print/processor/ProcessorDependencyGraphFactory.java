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
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for constructing {@link org.mapfish.print.processor.ProcessorDependencyGraph} instances.
 *
 * Created by Jesse on 3/24/14.
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
    public ProcessorDependencyGraph build(final List<Processor> processors) {
        ProcessorDependencyGraph graph = new ProcessorDependencyGraph();

        final Map<String, ProcessorGraphNode> provideBy = new HashMap<String, ProcessorGraphNode>();
        final List<ProcessorGraphNode> nodes = new ArrayList<ProcessorGraphNode>(processors.size());

        for (Processor processor : processors) {
            final Map<String, String> outputMapper = processor.getOutputMapper();

            for (String value : outputMapper.values()) {
                if (provideBy.containsKey(value)) {
                    throw new IllegalStateException("Multiple processors provide the same output value: '" + processor + "' and '" +
                                                    provideBy.get(value) + "' both provide: '" + value + "'");
                }

                final ProcessorGraphNode node = new ProcessorGraphNode(processor, this.metricRegistry);
                nodes.add(node);
                provideBy.put(value, node);
            }
        }

        for (ProcessorGraphNode node : nodes) {
            if (node.getProcessor().getInputMapper().isEmpty()) {
                graph.addRoot(node);
            } else {
                for (String requiredKey : node.getProcessor().getInputMapper().keySet()) {
                    final ProcessorGraphNode solution = provideBy.get(requiredKey);
                    solution.addDependency(node);
                }
            }
        }
        return graph;
    }

}
