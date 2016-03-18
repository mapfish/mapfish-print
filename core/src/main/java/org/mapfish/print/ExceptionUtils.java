package org.mapfish.print;


/**
 * Util class for exception handling.
 */
public final class ExceptionUtils {

    private ExceptionUtils() { }

    /**
     * Returns a {@link RuntimeException} for the given exception.
     *
     * @param exc An exception.
     * @return A {@link RuntimeException}
     */
    public static RuntimeException getRuntimeException(final Throwable exc) {
        Throwable e = exc;
        while (e.getCause() instanceof RuntimeException) {
            e = e.getCause();
        }
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(exc);
        }
    }
}
