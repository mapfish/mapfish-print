package org.mapfish.print.wrapper.json;

import org.mapfish.print.PrintException;
import org.mapfish.print.wrapper.PElement;

/**
 * Thrown when an attribute has an invalid value in the spec.
 */
public class InvalidJsonValueException extends PrintException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param element element that was queried
     * @param key key that was desired
     * @param value the illegal value obtained.
     */
    public InvalidJsonValueException(final PElement element, final String key, final Object value) {
        this(element, key, value, null);
    }

    /**
     * Constructor.
     *
     * @param element element that was queried
     * @param key key that was desired
     * @param value the illegal value obtained.
     * @param e the exception to wrap by this exception
     */
    public InvalidJsonValueException(
            final PElement element, final String key, final Object value, final Throwable e) {
        super(element.getPath(key) + " has an invalid value: " + value.toString(), e);
    }

    @Override
    public final String toString() {
        if (getCause() != null) {
            return super.toString() + " (" + getCause().getMessage() + ")";
        } else {
            return super.toString();
        }
    }
}
