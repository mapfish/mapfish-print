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

import org.mapfish.print.output.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import javax.annotation.Nonnull;

/**
 * Represents a graph of the processors dependencies.  The root nodes can execute in parallel but processors with
 * dependencies must wait for their dependencies to complete before execution.
 * <p/>
 * Created by Jesse on 3/24/14.
 */
public final class ProcessorDependencyGraph {
    private final List<ProcessorGraphNode> roots;

    ProcessorDependencyGraph() {
        this.roots = new ArrayList<ProcessorGraphNode>();
    }

    /**
     * Create a ForkJoinTask for running in a fork join pool.
     *
     * @param values the values to use for getting the required inputs of the processor and putting the output in.
     * @return a task ready to be submitted to a fork join pool.
     */
    public ProcessorGraphForkJoinTask createTask(@Nonnull final Values values) {
        return new ProcessorGraphForkJoinTask(values);
    }


    void addRoot(final ProcessorGraphNode node) {
        this.roots.add(node);
    }

    /**
     * A ForkJoinTask that will create ForkJoinTasks from each root and run each of them.
     */
    public final class ProcessorGraphForkJoinTask extends RecursiveTask<Values> {
        private final Values values;

        private ProcessorGraphForkJoinTask(@Nonnull final Values values) {
            this.values = values;
        }

        @Override
        protected Values compute() {
            final ProcessorDependencyGraph graph = ProcessorDependencyGraph.this;

            final List<ProcessorGraphNode> dependencyNodes = graph.roots;
            if (!dependencyNodes.isEmpty()) {
                List<ForkJoinTask<Values>> tasks = new ArrayList<ForkJoinTask<Values>>(dependencyNodes.size());

                // fork all but 1 dependencies (the first will be ran in current thread)
                for (int i = 1; i < dependencyNodes.size(); i++) {
                    final ForkJoinTask<Values> task = dependencyNodes.get(i).createTask(this.values);
                    tasks.add(task);
                    task.fork();
                }

                // compute one task in current thread so as not to waste threads
                dependencyNodes.get(0).createTask(this.values).compute();

                for (ForkJoinTask<Values> task : tasks) {
                    task.join();
                }
            }

            return this.values;
        }
    }
}
