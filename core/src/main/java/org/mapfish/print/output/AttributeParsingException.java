package org.mapfish.print.output;

/**
 * An exception that is thrown if an unexpected error occurs while parsing attributes into the Values object.
 *
 * @see Values#populateFromAttributes(org.mapfish.print.config.Template, java.util.Map, org.mapfish.print.wrapper.PObject)
 */
public final class AttributeParsingException extends RuntimeException {

    /**
     * Constructor.
     * @param errorMsg the message with debug info.
     * @param cause the exception that was thrown.
     */
    public AttributeParsingException(final String errorMsg, final Throwable cause) {
        super(errorMsg, cause);
    }
}
