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

import jsr166y.ForkJoinTask;
import jsr166y.RecursiveTask;

import org.mapfish.print.config.Template;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorDependencyGraph.ProcessorGraphForkJoinTask;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * Created by Jesse on 3/25/14.
 */
public class ExecuteIterProcessorsTask extends RecursiveTask<List<Map<String, ?>>> {
    private final Values values;
    private final Template template;
    private final List<ProcessorGraphForkJoinTask> forkedTasks;
    private final ClientHttpRequestFactory httpRequestFactory;
    private final File taskDirectory;

    /**
     * Constructor.
     *
     * @param values the values after normal processor have ran
     * @param template the current template
     * @param httpRequestFactory a factory for making http requests.
     * @param taskDirectory the temporary directory for this printing task.
     */
    public ExecuteIterProcessorsTask(final Values values, final Template template,
            final ClientHttpRequestFactory httpRequestFactory, final File taskDirectory) {
        this.values = values;
        this.template = template;
        this.forkedTasks = new LinkedList<ProcessorDependencyGraph.ProcessorGraphForkJoinTask>();
        this.httpRequestFactory = httpRequestFactory;
        this.taskDirectory = taskDirectory;
    }

    @Override
    public final boolean cancel(final boolean mayInterruptIfRunning) {
        synchronized (this.forkedTasks) {
            for (ProcessorGraphForkJoinTask task : this.forkedTasks) {
                task.cancel(mayInterruptIfRunning);
            }
        }
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    protected final List<Map<String, ?>> compute() {
        Iterable<Values> iterValues = this.values.getIterator(this.template.getIterValue());
        List<Map<String, ?>> dataSource = new ArrayList<Map<String, ?>>();
        final ProcessorDependencyGraph processorGraph = this.template.getIterProcessorGraph();

        // the tasks are created in a synchronized block to ensure that all
        // tasks get canceled, if cancel() is called
        synchronized (this.forkedTasks) {
            checkCancelState();
            for (Values v : iterValues) {
                v.put(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, this.httpRequestFactory);
                v.put(Values.TASK_DIRECTORY_KEY, this.taskDirectory);
                this.forkedTasks.add(processorGraph.createTask(v));
            }
        }

        // only when all tasks are created, fork them
        for (ForkJoinTask<Values> fork : this.forkedTasks) {
            fork.fork();
        }

        // then wait until all graphs are processed
        for (ForkJoinTask<Values> fork : this.forkedTasks) {
            checkCancelState();
            final Values v = fork.join();
            dataSource.add(v.getParameters());
        }

        return dataSource;
    }

    private void checkCancelState() {
        if (isCancelled()) {
            throw new CancellationException("task was canceled");
        }
    }
}
