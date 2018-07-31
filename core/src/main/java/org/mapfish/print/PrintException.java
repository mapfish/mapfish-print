package org.mapfish.print;

/**
 * Base exception for printing problems.
 */
public class PrintException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Construct exception.
     *
     * @param message the message
     */
    public PrintException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message message
     * @param cause the cause
     */
    public PrintException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
