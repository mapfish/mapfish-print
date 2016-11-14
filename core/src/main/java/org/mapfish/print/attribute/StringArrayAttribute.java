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
}
