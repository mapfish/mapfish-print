package org.mapfish.print.attribute;

/** An integer type attribute. [[examples=verboseExample]] */
public class IntegerAttribute extends PrimitiveAttribute<Integer> {

  /** Constructor. */
  public IntegerAttribute() {
    super(Integer.class);
  }

  /**
   * A default value for this attribute. Example:
   *
   * <pre><code>
   *     attributes:
   *       title: !integer
   *         default: 42</code></pre>
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
