package org.mapfish.print.processor;

import java.util.Map;
import org.mapfish.print.attribute.Attribute;

/** Processor that provide attributes. */
public interface ProvideAttributes {
  /**
   * The provides attribute by the processor.
   *
   * @return the provided attributes
   */
  Map<String, Attribute> getAttributes();
}
