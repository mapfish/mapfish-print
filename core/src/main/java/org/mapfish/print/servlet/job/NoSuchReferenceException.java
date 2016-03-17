package org.mapfish.print.servlet.job;

/**
 * Exception thrown for invalid job references.
 */
public class NoSuchReferenceException extends Exception {

    private static final long serialVersionUID = 6066917028707184891L;

    /**
     * Constructor.
     * @param msg error message
     */
    public NoSuchReferenceException(final String msg) {
        super(msg);
    }

}
