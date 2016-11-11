package org.mapfish.print.servlet;

/**
 * An exception that is thrown when a client requests an "app" that does not exist.
 */
public class NoSuchAppException extends Exception {
    NoSuchAppException(final String message) {
        super(message);
    }
}
