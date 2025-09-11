package org.mapfish.print.config;

import static org.junit.Assert.assertNotNull;

import com.codahale.metrics.MetricRegistry;
import java.util.List;
import jakarta.annotation.Nullable;
import org.mapfish.print.processor.AbstractProcessor;
import org.springframework.beans.factory.annotation.Autowired;

/** Test Processor. */
public class ProcessorWithSpringInjection extends AbstractProcessor<Object, Void> {

  @Autowired private MetricRegistry registry;

  /** Constructor. */
  protected ProcessorWithSpringInjection() {
    super(Void.class);
  }

  public void assertInjected() {
    assertNotNull(registry);
  }

  @Override
  public Object createInputParameter() {
    return null;
  }

  @Nullable
  @Override
  public Void execute(final Object values, final ExecutionContext context) {
    return null;
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration configuration) {
    // no checks
  }
}
