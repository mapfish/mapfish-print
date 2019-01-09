package org.mapfish.print;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utilities for Java's Optional class.
 */
public final class OptionalUtils {
    private OptionalUtils() {
    }

    /**
     * Return the first optional to be defined.
     *
     * @param a The first
     * @param b The second
     * @param <T> The type
     * @return a or b or empty
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> or(final Optional<T> a, final Optional<? extends T> b) {
        return a.isPresent() ? a : (Optional<T>) b;
    }

    /**
     * Return the first optional to be defined.
     *
     * @param optionals the lambdas returning the optionals
     * @param <T> The type
     * @return the first non-empty or empty
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> Optional<T> or(final Supplier<Optional<? extends T>>... optionals) {
        return (Optional<T>) Arrays.stream(optionals)
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .findFirst()
                .orElseGet(Optional::empty);
    }
}
