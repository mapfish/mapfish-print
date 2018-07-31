package org.mapfish.print.processor.map.scalebar;

import java.awt.Graphics2D;

/**
 * Draw a bar with alternating black and white zones marking the sub-intervals. Intervals have small
 * additional ticks.
 */
public class BarSubScalebarDrawer extends BarScalebarDrawer {

    /**
     * Constructor.
     *
     * @param graphics2d The graphics context.
     * @param settings Parameters for rendering the scalebar.
     */
    public BarSubScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        super(graphics2d, settings);
    }

    @Override
    protected final void drawBar() {
        super.drawBar();

        for (int i = 0; i <= getParams().intervals; ++i) {
            if (getSettings().getLabels().get(i).getLabelLayout().getCharacterCount() > 0) {
                float pos = i * getSettings().getIntervalLengthInPixels();
                getGraphics2d().drawLine(Math.round(pos), 0, Math.round(pos),
                                         Math.round(getSettings().getLineWidth() * 1.5f));
            }
        }
    }
}
