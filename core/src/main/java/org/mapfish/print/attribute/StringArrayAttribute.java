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
        super((Class<String[]>) new String[0].getClass());
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
}
