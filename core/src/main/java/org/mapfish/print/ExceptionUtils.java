package org.mapfish.print;


/**
 * Util class for exception handling.
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    }

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

    /**
     * Because exceptions might get re-thrown several times, an error message like
     * "java.util.concurrent.ExecutionException: java.lang.IllegalArgumentException:
     * java.lang.IllegalArgumentException: ..." might get created. To avoid this, this method finds the root
     * cause, so that only a message like "java.lang.IllegalArgumentException: ..." is shown.
     *
     * @param e A throwable.
     * @return root Throwable
     */
    public static Throwable getRootCause(final Throwable e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
}
