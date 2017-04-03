package org.mapfish.print.processor;

import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor.Context;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Contains information shared across all nodes being executed.
 * <p></p>
 */
public final class ProcessorExecutionContext {
    private final Values values;
    private final IdentityHashMap<Processor, Void> runningProcessors = new IdentityHashMap<Processor, Void>();
    private final IdentityHashMap<Processor, Void> executedProcessors = new IdentityHashMap<Processor, Void>();
    private final Lock processorLock = new ReentrantLock();
    private final Context context;

    /**
     * Constructor.
     *
     * @param values the values object.
     */
    public ProcessorExecutionContext(final Values values) {
        this.values = values;
        this.context = new Context();
    }

    public Values getValues() {
        return this.values;
    }

    /**
     * Try to start the node of a processor.
     *
     * In case the processor is already running, has already finished or if not all
     * requirements have finished, the processor can not start.
     * If the above conditions are fulfilled, the processor is added to the list of
     * running processors, and is expected to be started from the caller.
     *
     * @param processorGraphNode the node that should start.
     * @return if the node of the processor can be started.
     */
    @SuppressWarnings("unchecked")
    public boolean tryStart(final ProcessorGraphNode processorGraphNode) {
        this.processorLock.lock();
        boolean canStart = false;
        try {
            if (isRunning(processorGraphNode) || isFinished(processorGraphNode) || !allAreFinished(processorGraphNode.getRequirements())) {
                canStart = false;
            } else {
                started(processorGraphNode);
                canStart = true;
            }
        } finally {
            this.processorLock.unlock();
        }

        return canStart;
    }

    /**
     * Flag that the processor has started execution.
     *
     * @param processorGraphNode the node that has started.
     */
    private void started(final ProcessorGraphNode processorGraphNode) {
        this.processorLock.lock();
        try {
            this.runningProcessors.put(processorGraphNode.getProcessor(), null);
        } finally {
            this.processorLock.unlock();
        }
    }

    /**
     * Return true if the processor of the node is currently being executed.
     *
     * @param processorGraphNode the node to test.
     */
    public boolean isRunning(final ProcessorGraphNode processorGraphNode) {
        this.processorLock.lock();
        try {
            return this.runningProcessors.containsKey(processorGraphNode.getProcessor());
        } finally {
            this.processorLock.unlock();
        }
    }

    /**
     * Return true if the processor of the node has previously been executed.
     *
     * @param processorGraphNode the node to test.
     */
    public boolean isFinished(final ProcessorGraphNode processorGraphNode) {
        this.processorLock.lock();
        try {
            return this.executedProcessors.containsKey(processorGraphNode.getProcessor());
        } finally {
            this.processorLock.unlock();
        }
    }

    /**
     * Flag that the processor has completed execution.
     *
     * @param processorGraphNode the node that has finished.
     */
    public void finished(final ProcessorGraphNode processorGraphNode) {
        this.processorLock.lock();
        try {
            this.runningProcessors.remove(processorGraphNode.getProcessor());
            this.executedProcessors.put(processorGraphNode.getProcessor(), null);
        } finally {
            this.processorLock.unlock();
        }
    }

    /**
     * Verify that all processors have finished executing atomically (within the same lock as {@link #finished(ProcessorGraphNode)}
     * and {@link #isFinished(ProcessorGraphNode)} is within.
     *
     * @param processorNodes the node to check for completion.
     */
    public boolean allAreFinished(final Set<ProcessorGraphNode<?, ?>> processorNodes) {
        this.processorLock.lock();
        try {
            for (ProcessorGraphNode<?, ?> node : processorNodes) {
                if (!isFinished(node)) {
                    return false;
                }
            }
            return true;
        } finally {
            this.processorLock.unlock();
        }

    }

    /**
     * Set a {@code cancel} flag.
     *
     * All processors are supposed to check this flag frequently
     * and terminate the execution if requested.
     */
    public void cancel() {
        this.context.cancel();
    }

    public Context getContext() {
        return this.context;
    }

    public String getJobId() {
        return this.values.getString(Values.JOB_ID_KEY);
    }
}
