package org.mapfish.print.processor.map.scalebar;

/**
 * Specify a orientation for the labels and the bar.
 */
public enum Orientation {
    /**
     * Horizontal scalebar and the labels are shown below the bar.
     */
    HORIZONTAL_LABELS_BELOW("horizontalLabelsBelow", true),

    /**
     * Horizontal scalebar and the labels are shown above the bar.
     */
    HORIZONTAL_LABELS_ABOVE("horizontalLabelsAbove", true),

    /**
     * Vertical scalebar and the labels are shown left of the bar.
     */
    VERTICAL_LABELS_LEFT("verticalLabelsLeft", false),

    /**
     * Vertical scalebar and the labels are shown right of the bar.
     */
    VERTICAL_LABELS_RIGHT("verticalLabelsRight", false);

    private final String label;
    private final boolean horizontal;

    Orientation(final String label, final boolean horizontal) {
        this.label = label;
        this.horizontal = horizontal;
    }

    /**
     * Get a direction from its label.
     *
     * @param label the direction label
     */
    public static Orientation fromString(final String label) {
        if (label != null) {
            for (Orientation direction: Orientation.values()) {
                if (label.equalsIgnoreCase(direction.label)) {
                    return direction;
                }
            }
        }
        return null;
    }

    public final boolean isHorizontal() {
        return this.horizontal;
    }

    public final String getLabel() {
        return this.label;
    }
}
