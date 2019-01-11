package org.mapfish.print;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Some utilities for Sets.
 */
public final class SetsUtils {
    private SetsUtils() {
    }

    /**
     * Create a HashSet with the given initial values.
     *
     * @param values The values
     * @param <T> The type.
     */
    @SafeVarargs
    public static <T> Set<T> create(final T... values) {
        Set<T> result = new HashSet<>(values.length);
        Collections.addAll(result, values);
        return result;
    }
}
