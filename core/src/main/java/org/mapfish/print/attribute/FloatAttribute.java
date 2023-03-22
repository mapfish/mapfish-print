package org.mapfish.print.attribute;

/** A float type attribute. [[examples=verboseExample]] */
public class FloatAttribute extends PrimitiveAttribute<Double> {
  /** Constructor. */
  public FloatAttribute() {
    super(Double.class);
  }

  /**
   * A default value for this attribute. Example:
   *
   * <pre><code>
   *     attributes:
   *       title: !float
   *         default: 4.2</code></pre>
   *
   * @param value The default value.
   */
  public final void setDefault(final Double value) {
    this.defaultValue = value;
  }

  @Override
  public Class getValueType() {
    return Double.class;
  }
}
