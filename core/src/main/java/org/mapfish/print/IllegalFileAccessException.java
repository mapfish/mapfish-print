package org.mapfish.print;

/**
 * Indicates there was an attempt to load a file that was not in the configuration directory.
 */
public class IllegalFileAccessException extends RuntimeException {
    /**
     * Constructor.
     *
     * @param msg the error message
     */
    public IllegalFileAccessException(final String msg) {
        super(msg);
    }
}
