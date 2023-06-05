package org.mapfish.print.config;

import static org.junit.Assert.assertNotNull;

import com.codahale.metrics.MetricRegistry;
import org.mapfish.print.attribute.PrimitiveAttribute;
import org.springframework.beans.factory.annotation.Autowired;

/** Test Attribute. */
public class AttributeWithSpringInjection extends PrimitiveAttribute<Integer> {

  @Autowired private MetricRegistry registry;

  /** Constructor. */
  public AttributeWithSpringInjection() {
    super(Integer.class);
  }

  public void assertInjected() {
    assertNotNull(registry);
  }

  /**
   * A default value for this attribute.
   *
   * @param value The default value.
   */
  public final void setDefault(final Integer value) {
    this.defaultValue = value;
  }

  @Override
  public Class getValueType() {
    return Integer.class;
  }
}
