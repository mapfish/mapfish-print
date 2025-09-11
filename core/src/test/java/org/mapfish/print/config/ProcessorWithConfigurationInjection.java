package org.mapfish.print.config;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import jakarta.annotation.Nullable;
import org.mapfish.print.processor.AbstractProcessor;

/** Processor that needs the configuration object injected. */
public class ProcessorWithConfigurationInjection extends AbstractProcessor<Object, Void>
    implements HasConfiguration {

  private Configuration configuration;

  /** Constructor. */
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
  public Void execute(final Object values, final ExecutionContext context) {
    return null;
  }

  @Override
  public Object createInputParameter() {
    return null;
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration configuration) {
    // no checks
  }
}
