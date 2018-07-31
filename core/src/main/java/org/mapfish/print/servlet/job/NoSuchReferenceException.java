package org.mapfish.print.servlet.job;

/**
 * Exception thrown for invalid job references.
 */
public class NoSuchReferenceException extends Exception {

    private static final long serialVersionUID = 6066917028707184891L;

    /**
     * Constructor.
     *
     * @param referenceId the reference id
     */
    public NoSuchReferenceException(final String referenceId) {
        super("invalid reference '" + referenceId + "'");
    }

}
