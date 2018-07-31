package org.mapfish.print.attribute;


/**
 * <p>An integer type attribute.</p>
 * [[examples=verboseExample]]
 */
public class IntegerAttribute extends PrimitiveAttribute<Integer> {

    /**
     * Constructor.
     */
    public IntegerAttribute() {
        super(Integer.class);
    }

    /**
     * <p>A default value for this attribute. Example:</p>
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
