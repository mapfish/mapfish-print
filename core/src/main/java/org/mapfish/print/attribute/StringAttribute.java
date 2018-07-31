package org.mapfish.print.attribute;


/**
 * <p>Attribute that reads a string from the request data.</p>
 * [[examples=verboseExample]]
 */
public class StringAttribute extends PrimitiveAttribute<String> {

    private int maxLength = -1;

    /**
     * Constructor.
     */
    public StringAttribute() {
        super(String.class);
    }

    /**
     * <p>A default value for this attribute. Example:</p>
     * <pre><code>
     *     attributes:
     *       title: !string
     *         default: The title</code></pre>
     *
     * @param value The default value.
     */
    public final void setDefault(final String value) {
        this.defaultValue = value;
    }

    public final int getMaxLength() {
        return this.maxLength;
    }

    /**
     * The maximum number of characters allowed for this field (default: unlimited).
     *
     * @param maxLength Maximum number of characters.
     */
    public final void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public final void validateValue(final Object value) {
        if (this.maxLength >= 0 && value instanceof String) {
            String text = (String) value;
            if (text.length() > this.maxLength) {
                throw new IllegalArgumentException(
                        "text contains more than " + this.maxLength + " characters");
            }
        }
    }

    @Override
    public Class getValueType() {
        return String.class;
    }
}
