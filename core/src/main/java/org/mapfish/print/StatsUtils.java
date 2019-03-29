package org.mapfish.print;

/**
 * Utility functions for metrics.
 */
public final class StatsUtils {
    private StatsUtils() {
    }

    /**
     * Convert the given name into a proper metric part (what lies between dots).
     *
     * @param name the name
     */
    public static String quotePart(final String name) {
        if (name == null) {
            return "NULL";
        }
        return name.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
