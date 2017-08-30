package org.mapfish.print.processor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.output.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

/**
 * Represents one node in the Processor dependency graph ({@link ProcessorDependencyGraph}).
 * <p></p>
 *
 * @param <In> Same as {@link org.mapfish.print.processor.Processor} <em>In</em> parameter
 * @param <Out> Same as {@link org.mapfish.print.processor.Processor} <em>Out</em> parameter
 */
public final class ProcessorGraphNode<In, Out> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorGraphNode.class);
    private final Processor<In, Out> processor;
    private final Set<ProcessorGraphNode> dependencies = Sets.newHashSet();
    private final Set<ProcessorGraphNode> requirements = Sets.newHashSet();
    private final MetricRegistry metricRegistry;

    /**
     * Constructor.
     *
     * @param processor The processor associated with this node.
     * @param metricRegistry registry for timing the execution time of the processor.
     */
    public ProcessorGraphNode(
            @Nonnull final Processor<In, Out> processor,
            @Nonnull final MetricRegistry metricRegistry) {
        this.processor = processor;
        this.metricRegistry = metricRegistry;
    }

    public Processor<?, ?> getProcessor() {
        return this.processor;
    }

    /**
     * Add a dependency to this node.
     *
     * @param node the dependency to add.
     */
    public void addDependency(final ProcessorGraphNode node) {
        Assert.isTrue(node != this, "A processor can't depends on himself");

        this.dependencies.add(node);
        node.addRequirement(this);
    }

    private void addRequirement(final ProcessorGraphNode node) {
        this.requirements.add(node);
    }

    protected Set<ProcessorGraphNode> getRequirements() {
        return this.requirements;
    }

    protected Set<ProcessorGraphNode> getDependencies() {
        return this.dependencies;
    }

    /**
     * Returns true if the node has requirements, that is there are other
     * nodes that should be run first.
     */
    public boolean hasRequirements() {
        return !this.requirements.isEmpty();
    }

    /**
     * Create a ForkJoinTask for running in a fork join pool.
     *
     * @param execContext the execution context, used for tracking certain aspects of the execution.
     * @return a task ready to be submitted to a fork join pool.
     */
    public Optional<ProcessorNodeForkJoinTask<In, Out>> createTask(
            @Nonnull final ProcessorExecutionContext execContext) {
        if (!execContext.tryStart(this)) {
            return Optional.absent();
        } else {
            return Optional.of(new ProcessorNodeForkJoinTask<In, Out>(this, execContext));
        }
    }

    /**
     * Get the output mapper from processor.
     */
    @Nonnull
    public BiMap<String, String> getOutputMapper() {
        final BiMap<String, String> outputMapper = this.processor.getOutputMapperBiMap();
        if (outputMapper == null) {
            return HashBiMap.create();
        }
        return outputMapper;
    }

    /**
     * Return input mapper from processor.
     */
    @Nonnull
    public BiMap<String, String> getInputMapper() {
        final BiMap<String, String> inputMapper = this.processor.getInputMapperBiMap();
        if (inputMapper == null) {
            return HashBiMap.create();
        }
        return inputMapper;
    }

    /**
     * Create a string representing this node.
     *
     * @param builder the builder to add the string to.
     * @param indent the number of steps of indent for this node
     */
    public void toString(final StringBuilder builder, final int indent) {
        int spaces = (indent) * 2;
        for (int i = 0; i < spaces; i++) {
            builder.append(' ');
        }
        if (indent > 0) {
            builder.append("+-- ");
        }

        builder.append(this.processor);
        for (ProcessorGraphNode dependency : this.dependencies) {
            builder.append('\n');
            dependency.toString(builder, indent + 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder, 0);
        return builder.toString();
    }

    public String getName() {
        return this.processor.toString();
    }

    /**
     * Create a set containing all the processor at the current node and the entire subgraph.
     */
    public Set<? extends Processor<?, ?>> getAllProcessors() {
        IdentityHashMap<Processor<?, ?>, Void> all = new IdentityHashMap<Processor<?, ?>, Void>();
        all.put(this.getProcessor(), null);
        for (ProcessorGraphNode<?, ?> dependency : this.dependencies) {
            for (Processor<?, ?> p : dependency.getAllProcessors()) {
                all.put(p, null);
            }
        }
        return all.keySet();
    }

    /**
     * A ForkJoinTask that will run the processor and all of its dependencies.
     */
    public static final class ProcessorNodeForkJoinTask<In, Out> extends RecursiveTask<Values> {
        private final ProcessorExecutionContext execContext;
        private final ProcessorGraphNode<In, Out> node;

        private ProcessorNodeForkJoinTask(final ProcessorGraphNode<In, Out> node, final ProcessorExecutionContext execContext) {
            this.node = node;
            this.execContext = execContext;
        }

        @Override
        protected Values compute() {
            MDC.put("job_id", this.execContext.getJobId());
            final Values values = this.execContext.getValues();

            final Processor<In, Out> process = this.node.processor;
            final MetricRegistry registry = this.node.metricRegistry;
            final String name = String.format("%s.compute(): %s",
                    ProcessorGraphNode.class.getName(), process.getClass());
            Timer.Context timerContext = registry.timer(name).time();
            try {
                In inputParameter = ProcessorUtils.populateInputParameter(process, values);

                Out output;
                try {
                    LOGGER.debug("Executing process: {}", process);
                    output = process.execute(inputParameter, this.execContext.getContext());
                    LOGGER.debug("Succeeded in executing process: {}", process);
                } catch (Exception e) {
                    if (this.execContext.getContext().isCanceled()) {
                        // the processor is already canceled, so we don't care if something fails
                        throw new CancellationException();
                    } else {
                        LOGGER.error("Error while executing process: " + process, e);
                        throw ExceptionUtils.getRuntimeException(e);
                    }
                }

                if (output != null) {
                    ProcessorUtils.writeProcessorOutputToValues(output, process, values);
                }
            } finally {
                this.execContext.finished(this.node);
                final long processorTime = TimeUnit.MILLISECONDS.convert(
                        timerContext.stop(), TimeUnit.NANOSECONDS);
                LOGGER.info("Time taken to run processor: '{}' was {} ms",
                        process.getClass(), processorTime);
            }

            if (this.execContext.getContext().isCanceled()) {
                throw new CancellationException();
            }
            executeDependencyProcessors();

            return values;
        }

        private void executeDependencyProcessors() {
            final Set<ProcessorGraphNode> dependencyNodes = this.node.dependencies;

            List<ProcessorNodeForkJoinTask<?, ?>> tasks = new ArrayList<ProcessorNodeForkJoinTask<?, ?>>(
                    dependencyNodes.size());

            // fork all but 1 dependencies (the first will be ran in current thread)
            for (final ProcessorGraphNode<?, ?> depNode : dependencyNodes) {
                Optional<? extends ProcessorNodeForkJoinTask<?, ?>> task = depNode.createTask(
                        this.execContext);
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
