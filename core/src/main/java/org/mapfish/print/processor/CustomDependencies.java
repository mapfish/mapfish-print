package org.mapfish.print.processor;

import java.util.Collection;
import jakarta.annotation.Nonnull;

/**
 * Classes that implement this interface indicate what she dynamically depends on, for the "values"
 * input.
 *
 * <p>The test for this class will be part of {@link
 * org.mapfish.print.processor.jasper.MergeDataSourceProcessor} and {@link
 * org.mapfish.print.processor.jasper.DataSourceProcessor} tests .
 */
public interface CustomDependencies {
  /** Get what we dynamically depends on, for the "values" input. */
  @Nonnull
  Collection<String> getDependencies();
}
