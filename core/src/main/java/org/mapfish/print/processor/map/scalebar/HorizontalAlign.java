package org.mapfish.print.processor.map.scalebar;

/**
 * Vertical align.
 */
public enum HorizontalAlign {
    /**
     * Left.
     */
    LEFT("left"),

    /**
     * Center.
     */
    CENTER("center"),

    /**
     * Right.
     */
    RIGHT("right");

    private final String label;

    HorizontalAlign(final String label) {
        this.label = label;
    }

    /**
     * Get a type from its label.
     *
     * @param label the type label
     */
    public static HorizontalAlign fromString(final String label) {
        if (label != null) {
            for (HorizontalAlign type: HorizontalAlign.values()) {
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
