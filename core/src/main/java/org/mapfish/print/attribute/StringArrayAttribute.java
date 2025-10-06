package org.mapfish.print.attribute;

/** An attribute that can contain an array of strings. [[examples=verboseExample]] */
public class StringArrayAttribute extends PrimitiveAttribute<String[]> {
  /** Constructor. */
  public StringArrayAttribute() {
    super(String[].class);
  }

  /**
   * A default value for this attribute. Example:
   *
   * <pre><code>
   *     attributes:
   *       title: !stringArray
   *         default: [one, two, three]</code></pre>
   *
   * @param value The default value.
   */
  public final void setDefault(final String[] value) {
    this.defaultValue = value;
  }

  @Override
  public Class getValueType() {
    return String[].class;
  }
}
