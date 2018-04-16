package org.mapfish.print.attribute;

/**
 * <p>An attribute that can contain an array of strings.</p>
 * [[examples=verboseExample]]
 */
public class StringArrayAttribute extends PrimitiveAttribute<String[]> {
    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    public StringArrayAttribute() {
        super(String[].class);
    }

    /**
     * <p>A default value for this attribute. Example:</p>
     * <pre><code>
     *     attributes:
     *       title: !stringArray
     *         default: [one, two, three]</code></pre>
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
