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
import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
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
import java.util.UUID;

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
     * {@link /mapfish-spring-processors.xml}.
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

        final Map<String, ProcessorGraphNode<Object, Object>> provideBy =
                new HashMap<String, ProcessorGraphNode<Object, Object>>();
        final Map<String, Class<?>> outputTypes = new HashMap<String, Class<?>>();
        final List<ProcessorGraphNode<Object, Object>> nodes =
                new ArrayList<ProcessorGraphNode<Object, Object>>(processors.size());

        for (Processor<Object, Object> processor : processors) {
            final ProcessorGraphNode<Object, Object> node =
                    new ProcessorGraphNode<Object, Object>(processor, this.metricRegistry);
            for (OutputValue value : getOutputValues(node)) {
                String outputName = value.getName();
                if (provideBy.containsKey(outputName)) {
                    // there is already an output with the same name
                    if (value.canBeRenamed()) {
                        // if this is just a debug output, we can simply rename it
                        outputName = outputName + "_" + UUID.randomUUID().toString();
                    } else {
                        throw new IllegalArgumentException("Multiple processors provide the same output mapping: '" +
                                processor + "' and '" + provideBy.get(outputName) + "' both provide: '" + outputName +
                                "'.  You have to rename one of the outputs and the corresponding input so that" +
                                " there is no ambiguity with regards to the input a processor consumes.");
                    }
                }

                provideBy.put(outputName, node);
                outputTypes.put(outputName, value.getType());
            }
            nodes.add(node);
        }

        ArrayList<ProcessorDependency> allDependencies = Lists.newArrayList(this.dependencies);
        for (ProcessorGraphNode<Object, Object> node : nodes) {
            if (node.getProcessor() instanceof CustomDependencies) {
                CustomDependencies custom = (CustomDependencies) node.getProcessor();
                allDependencies.addAll(custom.createDependencies(nodes));
            }
        }
        final SetMultimap<ProcessorGraphNode<Object, Object>, InputValue> inputsForNodes = cacheInputsForNodes(nodes);
        for (ProcessorGraphNode<Object, Object> node : nodes) {
            final Set<InputValue> inputs = inputsForNodes.get(node);

            // check explicit, external dependencies between nodes
            checkExternalDependencies(allDependencies, node, nodes);

            // check input/output value dependencies
            for (InputValue input : inputs) {
                final ProcessorGraphNode<Object, Object> solution = provideBy.get(input.getName());
                if (solution != null && solution != node) {
                    // check that the provided output has the same type
                    final Class<?> inputType = input.getType();
                    final Class<?> outputType = outputTypes.get(input.getName());
                    if (inputType.isAssignableFrom(outputType)) {
                        solution.addDependency(node);
                    } else {
                        throw new IllegalArgumentException(
                                "Type conflict: Processor '" + solution.getName() + "' provides an output with name '"
                                + input.getName() + "' and of type '" + outputType + " ', while "
                                + "processor '" + node.getName() + "' expects an input of that name with type '"
                                + inputType + "'! Please rename one of the attributes in the mappings of the processors.");
                    }
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
            final List<ProcessorDependency> allDependencies,
            final ProcessorGraphNode<Object, Object> node,
            final List<ProcessorGraphNode<Object, Object>> nodes) {
        for (ProcessorDependency dependency : allDependencies) {
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
                            boolean allRequiredInputsInCommon = true;
                            for (String requiredInput : dependency.getCommonInputs()) {
                                // to make things more complicated: the common input attributes might have
                                // different names in the two nodes. e.g. for `CreateOverviewMapProcessor`
                                // the overview map is called `overviewMap`, but on the `SetStyleProcessor`
                                // the map is simply called `map`.
                                final String requiredNodeInput = getRequiredNodeInput(requiredInput);
                                final String dependentNodeInput = getDependentNodeInput(requiredInput);

                                final String mappedKey = getMappedKey(node, requiredNodeInput);
                                if (!getOriginalKey(dependentNode, mappedKey).equals(dependentNodeInput)) {
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

    /**
     * Get the name of the common input attribute for the dependent node.
     * 
     * E.g. "map;overviewMap" -> "overviewMap"
     * or   "map" -> "map"
     */
    private String getDependentNodeInput(final String requiredInput) {
        if (!requiredInput.contains(";")) {
            return requiredInput;
        } else {
            return requiredInput.substring(requiredInput.indexOf(";") + 1);
        }
    }

    /**
     * Get the name of the common input attribute for the required node.
     * 
     * E.g. "map;overviewMap" -> "map"
     * or   "map" -> "map"
     */
    private String getRequiredNodeInput(final String requiredInput) {
        if (!requiredInput.contains(";")) {
            return requiredInput;
        } else {
            return requiredInput.substring(0, requiredInput.indexOf(";"));
        }
    }

    private String getMappedKey(final ProcessorGraphNode<Object, Object> node, final String requiredInput) {
        String inputName = requiredInput;
        if (node.getInputMapper().containsValue(requiredInput)) {
            inputName = node.getInputMapper().inverse().get(requiredInput);
        }
        
        return inputName;
    }

    private String getOriginalKey(final ProcessorGraphNode<Object, Object> node, final String mappedKey) {
        String inputName = mappedKey;
        if (node.getInputMapper().containsKey(mappedKey)) {
            inputName = node.getInputMapper().get(mappedKey);
        }
        
        return inputName;
    }

    private SetMultimap<ProcessorGraphNode<Object, Object>, InputValue> cacheInputsForNodes(
            final List<ProcessorGraphNode<Object, Object>> nodes) {
        final SetMultimap<ProcessorGraphNode<Object, Object>, InputValue> inputsForNodes = HashMultimap.create();
        for (ProcessorGraphNode<Object, Object> node : nodes) {
            final Set<InputValue> inputs = getInputs(node);
            inputsForNodes.putAll(node, inputs);
        }
        return inputsForNodes;
    }

    private boolean hasNoneOrOnlyExternalInput(final ProcessorGraphNode<Object, Object> node, final Set<InputValue> inputs,
            final Map<String, ProcessorGraphNode<Object, Object>> provideBy) {
        if (inputs.isEmpty()) {
            return true;
        }

        for (InputValue input : inputs) {
            final ProcessorGraphNode<Object, Object> provider = provideBy.get(input.getName());
            if (provider != null && provider != node) {
                return false;
            }
        }
        return true;
    }

    private static Set<InputValue> getInputs(final ProcessorGraphNode<Object, Object> node) {
        final BiMap<String, String> inputMapper = node.getInputMapper();
        final Set<InputValue> inputs = Sets.newHashSet();

        final Object inputParameter = node.getProcessor().createInputParameter();
        if (inputParameter != null) {
            verifyAllMappingsMatchParameter(inputMapper.values(), inputParameter.getClass(),
                    "One or more of the input mapping values of '" + node + "'  do not match an input parameter.  The bad mappings are");

            final Collection<Field> allProperties = getAllAttributes(inputParameter.getClass());
            for (Field field : allProperties) {
                if (!inputMapper.containsValue(field.getName())) {
                    inputs.add(new InputValue(field.getName(), field.getType()));
                } else {
                    inputs.add(new InputValue(inputMapper.inverse().get(field.getName()), field.getType()));
                }
            }
        }

        return inputs;
    }

    private static Collection<OutputValue> getOutputValues(final ProcessorGraphNode<Object, Object> node) {
        final Map<String, String> outputMapper = node.getOutputMapper();
        final Set<OutputValue> values = Sets.newHashSet();

        final Set<String> mappings = outputMapper.keySet();
        final Class<?> paramType = node.getProcessor().getOutputType();
        verifyAllMappingsMatchParameter(mappings, paramType, "One or more of the output mapping keys of '" + node + "' do not match an " +
                                                             "output parameter.  The bad mappings are: ");
        final Collection<Field> allProperties = getAllAttributes(paramType);
        for (Field field : allProperties) {
            // if the field is annotated with @DebugValue, it can be renamed automatically in a
            // mapping in case of a conflict.
            final boolean canBeRenamed = field.getAnnotation(InternalValue.class) != null;
            String name = ProcessorUtils.getOutputValueName(node.getProcessor().getOutputPrefix(), outputMapper, field);
            values.add(new OutputValue(name, canBeRenamed, field.getType()));
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

    private static class InputValue {
        private final String name;
        private Class<?> type;

        public InputValue(final String name, final Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.name);
        }

        @Override
        public boolean equals(final Object obj) {
            return Objects.equal(this.name, ((InputValue) obj).name);
        }

        public final String getName() {
            return this.name;
        }

        public final Class<?> getType() {
            return this.type;
        }

        @Override
        public String toString() {
            return "InputValue{" +
                   "name='" + this.name + '\'' +
                   ", type=" + this.type.getSimpleName() +
                   '}';
        }
    }

    private static final class OutputValue extends InputValue {
        private final boolean canBeRenamed;

        private OutputValue(final String name, final boolean canBeRenamed, final Class<?> type) {
            super(name, type);
            this.canBeRenamed = canBeRenamed;
        }

        public boolean canBeRenamed() {
            return this.canBeRenamed;
        }
    }
}
