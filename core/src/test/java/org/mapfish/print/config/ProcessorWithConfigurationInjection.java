package org.mapfish.print.config;

import org.mapfish.print.processor.AbstractProcessor;

import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertNotNull;

/**
 * Processor that needs the configuration object injected.
 */
public class ProcessorWithConfigurationInjection extends AbstractProcessor<Object, Void>
        implements HasConfiguration {

    private Configuration configuration;

    /**
     * Constructor.
     */
    protected ProcessorWithConfigurationInjection() {
        super(Void.class);
    }

    public void assertInjected() {
        assertNotNull(configuration);
    }


    @Override
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Nullable
    @Override
    public Void execute(Object values, ExecutionContext context) {
        return null;
    }

    @Override
    public Object createInputParameter() {
        return null;
    }

    @Override
    protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
        // no checks
    }
}
