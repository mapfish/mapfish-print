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

import java.util.IdentityHashMap;

/**
 * Contains information shared across all nodes being executed.
 * <p/>
 * @author jesseeichar on 3/24/14.
 */
public final class ProcessorExecutionContext {
    private final Values values;
    private final IdentityHashMap<Processor, Void> executedProcessors = new IdentityHashMap<Processor, Void>();

    /**
     * Constructor.
     *
     * @param values the values object.
     */
    public ProcessorExecutionContext(final Values values) {
        this.values = values;
    }

    public Values getValues() {
        return this.values;
    }

    /**
     * Return true if the node has previously been executed.
     *
     * @param processorGraphNode the node to test.
     */
    public boolean isFinished(final ProcessorGraphNode processorGraphNode) {
        return this.executedProcessors.containsKey(processorGraphNode.getProcessor());
    }

    /**
     * Flag that the processor has completed execution.
     *
     * @param processorGraphNode the node that has finished.
     */
    public void finished(final ProcessorGraphNode processorGraphNode) {
        this.executedProcessors.put(processorGraphNode.getProcessor(), null);
    }
}
