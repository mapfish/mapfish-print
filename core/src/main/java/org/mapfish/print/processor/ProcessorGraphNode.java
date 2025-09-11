package org.mapfish.print.processor;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.PrintException;
import org.mapfish.print.output.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents one node in the Processor dependency graph ({@link ProcessorDependencyGraph}).
 *
 * @param <IN> Same as {@link Processor} <em>In</em> parameter
 * @param <OUT> Same as {@link Processor} <em>Out</em> parameter
 */
public final class ProcessorGraphNode<IN, OUT> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorGraphNode.class);
  private final Processor<IN, OUT> processor;

  /** The list of processors that get values from the output of this processor. */
  private final Set<ProcessorGraphNode<?, ?>> dependencies = new HashSet<>();

  /** The list of processors on which this processor gets its values from. */
  private final Set<ProcessorGraphNode> requirements = new HashSet<>();

  private final MetricRegistry metricRegistry;

  /**
   * Constructor.
   *
   * @param processor The processor associated with this node.
   * @param metricRegistry registry for timing the execution time of the processor.
   */
  public ProcessorGraphNode(
      @Nonnull final Processor<IN, OUT> processor, @Nonnull final MetricRegistry metricRegistry) {
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

  Set<ProcessorGraphNode> getRequirements() {
    return this.requirements;
  }

  Set<ProcessorGraphNode<?, ?>> getDependencies() {
    return this.dependencies;
  }

  /**
   * Create a ForkJoinTask for running in a fork join pool.
   *
   * @param execContext the execution context, used for tracking certain aspects of the execution.
   * @return a task ready to be submitted to a fork join pool.
   */
  public Optional<ProcessorNodeForkJoinTask<IN, OUT>> createTask(
      @Nonnull final ProcessorExecutionContext execContext) {
    if (!execContext.tryStart(this)) {
      return Optional.empty();
    } else {
      return Optional.of(new ProcessorNodeForkJoinTask<>(this, execContext));
    }
  }

  /** Get the output mapper from processor. */
  @Nonnull
  public BiMap<String, String> getOutputMapper() {
    final BiMap<String, String> outputMapper = this.processor.getOutputMapperBiMap();
    if (outputMapper == null) {
      return HashBiMap.create();
    }
    return outputMapper;
  }

  /** Return input mapper from processor. */
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
   * @param parent the parent node
   */
  public void toString(final StringBuilder builder, final int indent, final String parent) {
    this.processor.toString(builder, indent, parent);
    for (ProcessorGraphNode<?, ?> dependency : this.dependencies) {
      dependency.toString(builder, indent + 1, this.processor.toString());
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    toString(builder, 0, "?");
    return builder.toString();
  }

  public String getName() {
    return this.processor.toString();
  }

  /** Create a set containing all the processor at the current node and the entire subgraph. */
  public Set<? extends Processor<?, ?>> getAllProcessors() {
    IdentityHashMap<Processor<?, ?>, Void> all = new IdentityHashMap<>();
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
   *
   * @param <In> the type of the input parameter
   * @param <Out> the type of the output parameter
   */
  public static final class ProcessorNodeForkJoinTask<In, Out> extends RecursiveTask<Values> {
    private final ProcessorExecutionContext execContext;
    private final ProcessorGraphNode<In, Out> node;

    private ProcessorNodeForkJoinTask(
        final ProcessorGraphNode<In, Out> node, final ProcessorExecutionContext execContext) {
      this.node = node;
      this.execContext = execContext;
    }

    @Override
    protected Values compute() {
      return this.execContext
          .getContext()
          .mdcContext(
              () -> {
                final Values values = this.execContext.getValues();
                executeProcess(this.node.processor, values);
                this.execContext.getContext().stopIfCanceled();
                ProcessorDependencyGraph.tryExecuteNodes(
                    this.node.dependencies, this.execContext, true);

                return values;
              });
    }

    private void executeProcess(final Processor<In, Out> process, final Values values) {
      final String timerName =
          MetricRegistry.name(
              ProcessorGraphNode.class.getSimpleName(),
              "compute",
              process.getClass().getSimpleName());
      final Timer.Context timerContext = this.node.metricRegistry.timer(timerName).time();
      try {
        final In inputParameter = ProcessorUtils.populateInputParameter(process, values);

        Out output;
        try {
          LOGGER.debug("Executing process: {}", process);
          output = process.execute(inputParameter, this.execContext.getContext());
          LOGGER.debug("Succeeded in executing process: {}", process);
        } catch (RuntimeException e) {
          throw handleException(e, e, process, timerName);
        } catch (Exception e) {
          throw handleException(e, null, process, timerName);
        }

        if (output != null) {
          ProcessorUtils.writeProcessorOutputToValues(output, process, values);
        }
      } finally {
        this.execContext.finished(this.node);
        final long processorTime =
            TimeUnit.MILLISECONDS.convert(timerContext.stop(), TimeUnit.NANOSECONDS);
        LOGGER.info(
            "Time taken to run processor: '{}' was {} ms", process.getClass(), processorTime);
      }
    }

    private RuntimeException handleException(
        final Exception cause,
        final RuntimeException runtimeCause,
        final Processor<In, Out> process,
        final String timerName) {
      LOGGER.info("Error while executing process: {}", process, cause);
      // the processor is already canceled, so we don't care if something fails
      this.execContext.getContext().stopIfCanceled();
      this.node.metricRegistry.counter(MetricRegistry.name(timerName, "error")).inc();
      return Objects.requireNonNullElseGet(
          runtimeCause, () -> new PrintException("Failed to execute process:" + process, cause));
    }
  }
}
