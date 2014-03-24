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
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import org.mapfish.print.output.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;
import javax.annotation.Nonnull;

/**
 * Represents one node in the Processor dependency graph ({@link ProcessorDependencyGraph}).
 * <p/>
 * Created by Jesse on 3/24/14.
 */
public final class ProcessorGraphNode {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorGraphNode.class);
    private final Processor processor;
    private final List<ProcessorGraphNode> dependencies = new ArrayList<ProcessorGraphNode>();
    private final MetricRegistry metricRegistry;

    /**
     * Constructor.
     *
     * @param processor      The processor associated with this node.
     * @param metricRegistry registry for timing the execution time of the processor.
     */
    public ProcessorGraphNode(@Nonnull final Processor processor, @Nonnull final MetricRegistry metricRegistry) {
        this.processor = processor;
        this.metricRegistry = metricRegistry;
    }

    public Processor getProcessor() {
        return this.processor;
    }

    /**
     * Add a dependency to this node.
     *
     * @param node the dependency to add.
     */
    public void addDependency(final ProcessorGraphNode node) {
        this.dependencies.add(node);
    }

    /**
     * Create a ForkJoinTask for running in a fork join pool.
     *
     * @param execContext the execution context, used for tracking certain aspects of the execution.
     * @return a task ready to be submitted to a fork join pool.
     */
    public Optional<ProcessorNodeForkJoinTask> createTask(@Nonnull final ProcessorExecutionContext execContext) {
        if (execContext.isFinished(this)) {
            return Optional.absent();
        } else {
            return Optional.of(new ProcessorNodeForkJoinTask(execContext));
        }
    }

    /**
     * Get the output mapper from processor.
     */
    @Nonnull
    public Map<String, String> getOutputMapper() {
        final Map<String, String> outputMapper = this.processor.getOutputMapper();
        if (outputMapper == null) {
            return Collections.emptyMap();
        }
        return outputMapper;
    }

    /**
     * Return input mapper from processor.
     */
    @Nonnull
    public Map<String, String> getInputMapper() {
        final Map<String, String> inputMapper = this.processor.getInputMapper();
        if (inputMapper == null) {
            return Collections.emptyMap();
        }
        return inputMapper;
    }

    /**
     * A ForkJoinTask that will run the processor and all of its dependencies.
     */
    public final class ProcessorNodeForkJoinTask extends RecursiveTask<Values> {
        private final ProcessorExecutionContext execContext;

        private ProcessorNodeForkJoinTask(final ProcessorExecutionContext execContext) {
            this.execContext = execContext;
        }

        @Override
        protected Values compute() {
            final Values values = this.execContext.getValues();

            final Processor process = ProcessorGraphNode.this.processor;
            final MetricRegistry registry = ProcessorGraphNode.this.metricRegistry;
            Timer.Context timerContext = registry.timer(ProcessorGraphNode.class.getName() + "_compute():" +
                                                        process.getClass()).time();
            try {
                Map<String, Object> input = new HashMap<String, Object>();
                Map<String, String> inputMap = getInputMapper();
                for (String value : inputMap.keySet()) {
                    input.put(
                            inputMap.get(value),
                            values.getObject(value, Object.class));
                }

                Map<String, Object> output;
                try {
                    output = process.execute(input);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (output == null) {
                    output = new HashMap<String, Object>();
                }

                Map<String, String> outputMap = getOutputMapper();
                for (String value : outputMap.keySet()) {
                    values.put(
                            outputMap.get(value),
                            output.get(value));
                }

                executeDependencyProcessors();

                return values;
            } finally {
                this.execContext.finished(ProcessorGraphNode.this);
                final long processorTime = timerContext.stop();
                LOGGER.debug("Time taken to run processor: '" + process.getClass() + "' was " + processorTime + " ms");
            }
        }

        private void executeDependencyProcessors() {
            final List<ProcessorGraphNode> dependencyNodes = ProcessorGraphNode.this.dependencies;

            List<ProcessorNodeForkJoinTask> tasks = new ArrayList<ProcessorNodeForkJoinTask>(dependencyNodes.size());

            // fork all but 1 dependencies (the first will be ran in current thread)
            for (int i = 0; i < dependencyNodes.size(); i++) {
                Optional<ProcessorNodeForkJoinTask> task = dependencyNodes.get(i).createTask(this.execContext);
                if (task.isPresent()) {
                    tasks.add(task.get());
                    if (tasks.size() > 1) {
                        task.get().fork();
                    }
                }
            }

            if (!tasks.isEmpty()) {
                // compute one task in current thread so as not to waste threads
                tasks.get(0).compute();

                for (ProcessorNodeForkJoinTask task : tasks.subList(1, tasks.size())) {
                    task.join();
                }
            }
        }
    }
}
