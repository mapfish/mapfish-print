package org.mapfish.print.servlet;

/**
 * An exception that is thrown when a client requests an "app" that does not exist.
 * @author Jesse on 5/7/2014.
 */
public class NoSuchAppException extends Exception {
    NoSuchAppException(final String message) {
        super(message);
    }
}
