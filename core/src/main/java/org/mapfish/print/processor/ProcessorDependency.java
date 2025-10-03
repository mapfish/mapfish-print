package org.mapfish.print.processor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Models a dependency between two processors. The processor <code>dependent</code> requires the
 * processor <code>required</code>
 *
 * @param required The processor which is required to be executed before the other.
 * @param dependent The processor which requires the other to be executed first.
 * @param commonInputs The dependency is only enforced if the two processors have these inputs in
 *     common.
 */
public record ProcessorDependency(
    Class<? extends Processor<?, ?>> required,
    Class<? extends Processor<?, ?>> dependent,
    Set<String> commonInputs) {

  /**
   * Constructor. The processor <code>dependent</code> requires the processor <code>required</code>.
   */
  public ProcessorDependency(
      final Class<? extends Processor<?, ?>> required,
      final Class<? extends Processor<?, ?>> dependent,
      final Set<String> commonInputs) {
    this.required = required;
    this.dependent = dependent;
    this.commonInputs = new HashSet<>(commonInputs);
  }

  /**
   * Constructor. The processor <code>dependent</code> requires the processor <code>required</code>.
   *
   * @param required The processor which is required to be executed before the other.
   * @param dependent The processor which requires the other to be executed first.
   */
  public ProcessorDependency(
      final Class<? extends Processor<?, ?>> required,
      final Class<? extends Processor<?, ?>> dependent) {
    this(required, dependent, new HashSet<>());
  }

  /** Returns the processor which is required to be executed before the other. */
  @Override
  public Class<? extends Processor<?, ?>> required() {
    return this.required;
  }

  /** Returns the processor which requires the other to be executed first. */
  @Override
  public Class<? extends Processor<?, ?>> dependent() {
    return this.dependent;
  }

  /** The inputs that both processors must have in common. */
  @Override
  public Set<String> commonInputs() {
    return Collections.unmodifiableSet(this.commonInputs);
  }

  @Override
  public final String toString() {
    return "ProcessorDependency{"
        + "required="
        + this.required.getSimpleName()
        + ", dependent="
        + this.dependent.getSimpleName()
        + ", commonInputs="
        + this.commonInputs
        + '}';
  }
}
