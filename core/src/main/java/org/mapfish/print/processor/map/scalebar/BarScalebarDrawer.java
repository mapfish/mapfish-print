package org.mapfish.print.processor.map.scalebar;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Draw a bar with alternating black and white zones marking the sub-intervals.
 */
public class BarScalebarDrawer extends ScalebarDrawer {

    /**
     * Constructor.
     *
     * @param graphics2d The graphics context.
     * @param settings Parameters for rendering the scalebar.
     */
    public BarScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        super(graphics2d, settings);
    }

    @Override
    protected void drawBar() {
        final int barSize = getSettings().getBarSize();

        float subIntervalWidth =
                getSettings().getIntervalLengthInPixels() / getSettings().getNumSubIntervals();
        int intervalsTotal = getParams().intervals * getSettings().getNumSubIntervals();
        for (int i = 0; i < intervalsTotal; i++) {
            float pos = i * subIntervalWidth;
            final Color color = i % 2 == 0 ? getParams().getBarBgColor() : getParams().getColor();
            if (color != null) {
                getGraphics2d().setColor(color);
                getGraphics2d().fillRect(Math.round(pos), -barSize, Math.round(subIntervalWidth), barSize);
            }
        }

        getGraphics2d().setColor(getParams().getColor());
        getGraphics2d().drawRect(
                0, -barSize,
                Math.round(getSettings().getIntervalLengthInPixels() * getParams().intervals), barSize);
    }
}
