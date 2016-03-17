package org.mapfish.print.wrapper;

import org.mapfish.print.PrintException;

/**
 * Thrown when an attribute is missing in the spec.
 */
public class ObjectMissingException extends PrintException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param pJsonObject the json object queried
     * @param key the key that was expected to exist.
     */
    public ObjectMissingException(final PElement pJsonObject, final String key) {
        super("attribute [" + pJsonObject.getPath(key) + "] missing");
    }
}
