package org.mapfish.print.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.mapfish.print.attribute.PrimitiveAttribute;

/** Attribute that needs the configuration object injected. */
public class AttributeWithConfigurationInjection extends PrimitiveAttribute<Integer>
    implements HasConfiguration {

  private Configuration configuration;

  /** Constructor. */
  public AttributeWithConfigurationInjection() {
    super(Integer.class);
  }

  /**
   * A default value for this attribute.
   *
   * @param value The default value.
   */
  public final void setDefault(final Integer value) {
    this.defaultValue = value;
  }

  public void assertInjected() {
    assertNotNull(configuration);
  }

  @Override
  public void setConfiguration(final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Class getValueType() {
    return Integer.class;
  }
}
