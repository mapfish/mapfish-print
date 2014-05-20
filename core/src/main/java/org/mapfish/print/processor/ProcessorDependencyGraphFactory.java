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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.parser.ParserUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mapfish.print.parser.ParserUtils.getAllAttributes;

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
     * External dependencies between processor types.
     */
    @Autowired
    private List<ProcessorDependency> dependencies;

    /**
     * Sets the external dependencies between processors. Usually configured in 
     * {@link mapfish-spring-processors.xml}.
     * @param dependencies the dependencies
     */
    public void setDependencies(final List<ProcessorDependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Create a {@link ProcessorDependencyGraph}.
     *
     * @param processors the processors that will be part of the graph
     * @return a {@link org.mapfish.print.processor.ProcessorDependencyGraph} constructed from the passed in processors
     */
    @SuppressWarnings("unchecked")
    public ProcessorDependencyGraph build(final List<? extends Processor> processors) {
        ProcessorDependencyGraph graph = new ProcessorDependencyGraph();

        final Map<String, ProcessorGraphNode<Object, Object>> provideBy = new HashMap<String, ProcessorGraphNode<Object, Object>>();
        final List<ProcessorGraphNode<Object, Object>> nodes = new ArrayList<ProcessorGraphNode<Object, Object>>(processors.size());

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
        
        final SetMultimap<ProcessorGraphNode<Object, Object>, String> inputsForNodes = cacheInputsForNodes(nodes);
        for (ProcessorGraphNode<Object, Object> node : nodes) {
            final Set<String> inputs = inputsForNodes.get(node);

            // check explicit, external dependencies between nodes
            checkExternalDependencies(node, nodes, inputsForNodes);
            
            // check input/output value dependencies
            for (String requiredKey : inputs) {
                final ProcessorGraphNode<Object, Object> solution = provideBy.get(requiredKey);
                if (solution != null && solution != node) {
                    solution.addDependency(node);
                }
            }
        }
        
        // once all dependencies are discovered, select the root nodes
        for (ProcessorGraphNode<Object, Object> node : nodes) {
            // a root node is a node that has no requirements (that is no other node
            // should be executed before the node) and that has only external inputs
            if (!node.hasRequirements() && 
                    hasNoneOrOnlyExternalInput(node, inputsForNodes.get(node), provideBy)) {
                graph.addRoot(node);
            }
        }
        
        Assert.isTrue(graph.getAllProcessors().containsAll(processors), "'" + graph + "' does not contain all the processors: " +
                                                                        processors);

        return graph;
    }

    private void checkExternalDependencies(
            final ProcessorGraphNode<Object, Object> node,
            final List<ProcessorGraphNode<Object, Object>> nodes,
            final SetMultimap<ProcessorGraphNode<Object, Object>, String> inputsForNodes) {
        for (ProcessorDependency dependency : this.dependencies) {
            if (dependency.getRequired().equals(node.getProcessor().getClass())) {
                // this node is required by another processor type, let's see if there
                // is an actual processor of this type
                for (ProcessorGraphNode<Object, Object> dependentNode : nodes) {
                    if (dependency.getDependent().equals(dependentNode.getProcessor().getClass())) {
                        // this is the right processor type, let's check if the processors should have
                        // some inputs in common
                        if (dependency.getCommonInputs().isEmpty()) {
                            // no inputs in common required, just create the dependency
                            node.addDependency(dependentNode);
                        } else {
                            // we have to check if the two processors have the given inputs in common.
                            // for example if the input "map" is required, the mapped name for "map" for
                            // processor 1 is retrieved, e.g. "mapDef1". if processor 2 also has a mapped
                            // input with name "mapDef1", we add a dependency.
                            final Set<String> inputsForDependentNode = inputsForNodes.get(dependentNode);
                            
                            boolean allRequiredInputsInCommon = true;
                            for (String requiredInput : dependency.getCommonInputs()) {
                                final String mappedKey = getMappedKey(node, requiredInput);
                                
                                if (!inputsForDependentNode.contains(mappedKey)) {
                                    allRequiredInputsInCommon = false;
                                    break;
                                }
                            }
                            
                            if (allRequiredInputsInCommon) {
                                node.addDependency(dependentNode);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getMappedKey(final ProcessorGraphNode<Object, Object> node, final String requiredInput) {
        String inputName = requiredInput;
        if (node.getInputMapper().containsValue(requiredInput)) {
            inputName = node.getInputMapper().inverse().get(requiredInput);
        }
        
        return inputName;
    }

    private SetMultimap<ProcessorGraphNode<Object, Object>, String> cacheInputsForNodes(
            final List<ProcessorGraphNode<Object, Object>> nodes) {
        final SetMultimap<ProcessorGraphNode<Object, Object>, String> inputsForNodes = HashMultimap.create();
        for (ProcessorGraphNode<Object, Object> node : nodes) {
            final Set<String> inputs = getInputs(node);
            inputsForNodes.putAll(node, inputs);
        }
        return inputsForNodes;
    }

    private boolean hasNoneOrOnlyExternalInput(final ProcessorGraphNode<Object, Object> node, final Set<String> inputs,
            final Map<String, ProcessorGraphNode<Object, Object>> provideBy) {
        if (inputs.isEmpty()) {
            return true;
        }
        
        for (String input : inputs) {
            final ProcessorGraphNode<Object, Object> provider = provideBy.get(input);
            if (provider != null && provider != node) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> getInputs(final ProcessorGraphNode<Object, Object> node) {
        final BiMap<String, String> inputMapper = node.getInputMapper();
        final Set<String> inputs = Sets.newHashSet(inputMapper.keySet());

        final Object inputParameter = node.getProcessor().createInputParameter();
        if (inputParameter != null) {
            verifyAllMappingsMatchParameter(inputMapper.values(), inputParameter.getClass(),
                    "One or more of the input mapping values of '" + node + "'  do not match an input parameter.  The bad mappings are");

            final Collection<Field> allProperties = getAllAttributes(inputParameter.getClass());
            for (Field descriptor : allProperties) {
                if (!inputMapper.containsValue(descriptor.getName())) {
                    inputs.add(descriptor.getName());
                }
            }
        }

        return inputs;
    }

    private static Collection<String> getOutputValues(final ProcessorGraphNode<Object, Object> node) {
        final Map<String, String> outputMapper = node.getOutputMapper();
        final Set<String> values = Sets.newHashSet(outputMapper.values());

        final Set<String> mappings = outputMapper.keySet();
        final Class<?> paramType = node.getProcessor().getOutputType();
        verifyAllMappingsMatchParameter(mappings, paramType, "One or more of the output mapping keys of '" + node + "' do not match an " +
                                                             "output parameter.  The bad mappings are: ");
        final Collection<Field> allProperties = getAllAttributes(paramType);
        for (Field field : allProperties) {
            if (!outputMapper.containsKey(field.getName())) {
                values.add(field.getName());
            }
        }

        return values;
    }

    private static void verifyAllMappingsMatchParameter(final Set<String> mappings, final Class<?> paramType,
                                                        final String errorMessagePrefix) {
        final Set<String> attributeNames = ParserUtils.getAllAttributeNames(paramType);
        StringBuilder errors = new StringBuilder();
        for (String mapping : mappings) {
            if (!attributeNames.contains(mapping)) {
                errors.append("\n  * ").append(mapping);

            }
        }

        Assert.isTrue(0 == errors.length(), errorMessagePrefix + errors + listOptions(attributeNames) + "\n");
    }

    private static String listOptions(final Set<String> attributeNames) {
        StringBuilder msg = new StringBuilder("\n\nThe possible parameter names are:");
        for (String attributeName : attributeNames) {
            msg.append("\n  * ").append(attributeName);
        }
        return msg.toString();
    }

}
