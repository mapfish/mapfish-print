package org.mapfish.print;

/**
 * Indicates there was an attempt to load a file that was not in the configuration directory.
 *
 * @author Jesse on 7/29/2014.
 */
public class IllegalFileAccessException extends RuntimeException {
    /**
     * Constructor.
     * @param msg the error message
     */
    public IllegalFileAccessException(final String msg) {
        super(msg);
    }
}
