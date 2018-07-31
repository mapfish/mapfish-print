package org.mapfish.print.processor.map.scalebar;

import java.awt.Graphics2D;

/**
 * Scalebar type.
 */
public enum Type {
    /**
     * A simple line with ticks.
     */
    LINE("line") {
        @Override
        public ScalebarDrawer createDrawer(
                final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
            return new LineScalebarDrawer(graphics2d, settings);
        }
    },

    /**
     * A bar with alternating black and white zones marking the sub-intervals.
     */
    BAR("bar") {
        @Override
        public ScalebarDrawer createDrawer(
                final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
            return new BarScalebarDrawer(graphics2d, settings);
        }
    },

    /**
     * A bar with alternating black and white zones marking the sub-intervals. Intervals have small additional
     * ticks.
     */
    BAR_SUB("bar_sub") {
        @Override
        public ScalebarDrawer createDrawer(
                final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
            return new BarSubScalebarDrawer(graphics2d, settings);
        }
    };

    private final String label;

    Type(final String label) {
        this.label = label;
    }

    /**
     * Get a type from its label.
     *
     * @param label the type label
     */
    public static Type fromString(final String label) {
        if (label != null) {
            for (Type type: Type.values()) {
                if (label.equalsIgnoreCase(type.label)) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * Create a {@link ScalebarDrawer} instance for this type.
     *
     * @param graphics2d The graphics context.
     * @param settings Parameters for rendering the scalebar.
     */
    public abstract ScalebarDrawer createDrawer(Graphics2D graphics2d, ScaleBarRenderSettings settings);

    public final String getLabel() {
        return this.label;
    }
}
