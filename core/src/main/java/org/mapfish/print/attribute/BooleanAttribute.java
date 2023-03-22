package org.mapfish.print.attribute;

/** A boolean type attribute. [[examples=verboseExample]] */
public class BooleanAttribute extends PrimitiveAttribute<Boolean> {

  /** Constructor. */
  public BooleanAttribute() {
    super(Boolean.class);
  }

  /**
   * A default value for this attribute. Example:
   *
   * <pre><code>
   *     attributes:
   *       title: !boolean
   *         default: True</code></pre>
   *
   * @param value The default value.
   */
  public final void setDefault(final Boolean value) {
    this.defaultValue = value;
  }

  @Override
  public Class getValueType() {
    return Boolean.class;
  }
}
