package org.mapfish.print.processor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.parser.ParserUtils;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

/**
 * Basic functionality of a processor.  Mostly utility methods.
 *
 * @param <In> A Java bean input parameter object of the execute method. Object is populated from the
 *         {@link org.mapfish.print.output.Values} object.
 * @param <Out> A Java bean output/return object from the execute method. properties will be put into
 *         the {@link org.mapfish.print.output.Values} object so other processor can access the values.
 */
public abstract class AbstractProcessor<In, Out> implements Processor<In, Out> {
    private final BiMap<String, String> inputMapper = HashBiMap.create();
    private final BiMap<String, String> outputMapper = HashBiMap.create();

    private final Class<Out> outputType;
    private String prefix;
    private String inputPrefix;
    private String outputPrefix;

    /**
     * Constructor.
     *
     * @param outputType the type of the output of this processor.  Used to calculate processor
     *         dependencies.
     */
    protected AbstractProcessor(final Class<Out> outputType) {
        this.outputType = outputType;
    }

    @Override
    public final Class<Out> getOutputType() {
        return this.outputType;
    }

    @Override
    @Nonnull
    public final BiMap<String, String> getInputMapperBiMap() {
        return this.inputMapper;
    }

    /**
     * The prefix to apply to each value.  This provides a simple way to make all output values have unique
     * values.
     *
     * @param prefix the new prefix
     */
    public final void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public final String getInputPrefix() {
        return this.inputPrefix == null ? this.prefix : this.inputPrefix;
    }

    /**
     * The prefix to apply to each input value.  This provides a simple way to make all output values have
     * unique values.
     *
     * @param inputPrefix the new prefix
     */
    public final void setInputPrefix(final String inputPrefix) {
        this.inputPrefix = inputPrefix;
    }

    @Override
    public final String getOutputPrefix() {
        return this.outputPrefix == null ? this.prefix : this.outputPrefix;
    }

    /**
     * The prefix to apply to each output value.  This provides a simple way to make all output values have
     * unique values.
     *
     * @param outputPrefix the new prefix
     */
    public final void setOutputPrefix(final String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    @Override
    public void toString(final StringBuilder builder, final int indent, final String parent) {
        int spaces = (indent) * 2;
        for (int i = 0; i < spaces; i++) {
            builder.append(' ');
        }
        builder.append("\"");
        builder.append(parent.replace("\"", "\\\""));
        builder.append("\" -> \"");
        builder.append(toString().replace("\"", "\\\""));
        builder.append("\";\n");
    }

    /**
     * The input mapper.
     *
     * @param inputMapper the values.
     */
    public final void setInputMapper(@Nonnull final Map<String, String> inputMapper) {
        this.inputMapper.putAll(inputMapper);
    }

    @Nonnull
    @Override
    public final BiMap<String, String> getOutputMapperBiMap() {
        return this.outputMapper;
    }

    /**
     * The output mapper.
     *
     * @param outputMapper the values.
     */
    public final void setOutputMapper(@Nonnull final Map<String, String> outputMapper) {
        this.outputMapper.putAll(outputMapper);
    }

    @Override
    public final void validate(final List<Throwable> errors, final Configuration configuration) {
        final In inputParameter = createInputParameter();
        final Set<String> allInputAttributeNames;
        if (inputParameter != null) {
            allInputAttributeNames = ParserUtils.getAllAttributeNames(inputParameter.getClass());
        } else {
            allInputAttributeNames = Collections.emptySet();
        }
        for (String inputAttributeName: this.inputMapper.values()) {
            if (!allInputAttributeNames.contains(inputAttributeName)) {
                errors.add(new ConfigurationException(inputAttributeName + " is not defined in processor '"
                                                              + this + "'.  Check for typos. Options are " +
                                                              allInputAttributeNames));
            }
        }

        Set<String> allOutputAttributeNames = ParserUtils.getAllAttributeNames(getOutputType());
        for (String outputAttributeName: this.outputMapper.keySet()) {
            if (!allOutputAttributeNames.contains(outputAttributeName)) {
                errors.add(new ConfigurationException(outputAttributeName + " is not defined in processor " +
                                                              "'" + this +
                                                              "' as an output attribute.  Check for typos. " +
                                                              "Options are " +
                                                              allOutputAttributeNames));
            }
        }

        extraValidation(errors, configuration);
    }

    /**
     * Perform any extra validation a subclass may need to perform.
     *
     * @param validationErrors a list to add errors to so that all validation errors are reported as
     *         one.
     * @param configuration the containing configuration
     */
    protected abstract void extraValidation(
            List<Throwable> validationErrors, Configuration configuration);

    // CHECKSTYLE:OFF
    @Override
    public String toString() {
        String result = getClass().getSimpleName();
        final String inPrefix = getInputPrefix();
        if (inPrefix != null) {
            result += " in=" + inPrefix;
        }
        final String outPrefix = getOutputPrefix();
        if (outPrefix != null) {
            result += " out=" + outPrefix;
        }
        return result;
    }
    // CHECKSTYLE:ON

    /**
     * Default implementation of {@link org.mapfish.print.processor.Processor.ExecutionContext}.
     */
    public static final class Context implements ExecutionContext {
        private final String jobId;
        private volatile boolean canceled = false;
        private ExecutionStats stats = new ExecutionStats();

        /**
         * @param jobId The job ID.
         */
        public Context(final String jobId) {
            this.jobId = jobId;
        }


        /**
         * Sets the canceled flag.
         */
        public void cancel() {
            this.canceled = true;
        }

        @Override
        public void stopIfCanceled() {
            if (this.canceled) {
                throw new CancellationException("task was canceled");
            }
        }

        @Override
        public ExecutionStats getStats() {
            return this.stats;
        }

        @Override
        public String getJobId() {
            return this.jobId;
        }

        @Override
        public <T> T mdcContext(final Supplier<T> action) {
            this.stopIfCanceled();
            final String prev = MDC.get(MDC_JOB_ID_KEY);
            boolean changed = prev == null || (jobId != null && jobId.equals(prev));
            if (changed) {
                MDC.put(MDC_JOB_ID_KEY, this.jobId);
            }
            try {
                return action.get();
            } finally {
                if (changed) {
                    if (prev != null) {
                        MDC.put(MDC_JOB_ID_KEY, prev);
                    } else {
                        MDC.remove(MDC_JOB_ID_KEY);
                    }
                }
            }
        }

        @Override
        public <T> T mdcContextEx(final Callable<T> action) throws Exception {
            this.stopIfCanceled();
            final String prev = MDC.get(MDC_JOB_ID_KEY);
            boolean mdcChanged = prev == null || jobId.equals(prev);
            if (mdcChanged) {
                MDC.put(MDC_JOB_ID_KEY, this.jobId);
            }
            try {
                return action.call();
            } finally {
                if (mdcChanged) {
                    if (prev != null) {
                        MDC.put(MDC_JOB_ID_KEY, prev);
                    } else {
                        MDC.remove(MDC_JOB_ID_KEY);
                    }
                }
            }
        }
    }
}
