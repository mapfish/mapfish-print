package org.mapfish.print.processor.map.scalebar;

/**
 * Vertical align.
 */
public enum VerticalAlign {
    /**
     * Top.
     */
    TOP("top"),

    /**
     * Middle.
     */
    MIDDLE("middle"),

    /**
     * Bottom.
     */
    BOTTOM("bottom");

    private final String label;

    VerticalAlign(final String label) {
        this.label = label;
    }

    /**
     * Get a type from its label.
     *
     * @param label the type label
     */
    public static VerticalAlign fromString(final String label) {
        if (label != null) {
            for (VerticalAlign type: VerticalAlign.values()) {
                if (label.equalsIgnoreCase(type.label)) {
                    return type;
                }
            }
        }
        return null;
    }

    public final String getLabel() {
        return this.label;
    }
}
