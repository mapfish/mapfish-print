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

package org.mapfish.print.output;

import org.mapfish.print.config.Template;
import org.mapfish.print.processor.ProcessorDependencyGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * Created by Jesse on 3/25/14.
 */
public class ExecuteIterProcessorsTask extends RecursiveTask<List<Map<String, ?>>> {
    private final Values values;
    private final Template template;

    /**
     * Constructor.
     *
     * @param values the values after normal processor have ran
     * @param template the current template
     */
    public ExecuteIterProcessorsTask(final Values values, final Template template) {
        this.values = values;
        this.template = template;
    }

    @Override
    protected final List<Map<String, ?>> compute() {
        Iterable<Values> iterValues = this.values.getIterator(this.template.getIterValue());
        List<Map<String, ?>> dataSource = new ArrayList<Map<String, ?>>();
        final ProcessorDependencyGraph processorGraph = this.template.getIterProcessorGraph();

        final List<ForkJoinTask<Values>> forks = new ArrayList<ForkJoinTask<Values>>(processorGraph.getRoots().size());

        for (Values v : iterValues) {
            forks.add(processorGraph.createTask(v).fork());
        }
        for (ForkJoinTask<Values> fork : forks) {
            final Values v = fork.join();
            dataSource.add(v.getParameters());
        }

        return dataSource;
    }
}
