package org.mapfish.print.processor.map.scalebar;

import java.awt.Graphics2D;

/**
 * Draw a simple line with ticks.
 */
public class LineScalebarDrawer extends ScalebarDrawer {

    /**
     * Constructor.
     *
     * @param graphics2d The graphics context.
     * @param settings Parameters for rendering the scalebar.
     */
    public LineScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        super(graphics2d, settings);
    }

    @Override
    protected final void drawBar() {
        final int barSize = getSettings().getBarSize();

        // first tick
        getGraphics2d().drawLine(0, -barSize, 0, 0);

        // horizontal line
        int intervalsLength = Math.round(getSettings().getIntervalLengthInPixels() * getParams().intervals);
        getGraphics2d().drawLine(0, -barSize, intervalsLength, -barSize);

        // last tick
        getGraphics2d().drawLine(intervalsLength, -barSize, intervalsLength, 0);

        // draw the ticks for each interval
        for (int i = 0; i < getParams().intervals; i++) {
            float pos = i * getSettings().getIntervalLengthInPixels();
            if (i > 0) {
                getGraphics2d().drawLine(Math.round(pos), 0, Math.round(pos), -barSize);
            }
            // draw the ticks for the sub-intervals
            for (int j = 1; j < getSettings().getNumSubIntervals(); j++) {
                pos += getSettings().getIntervalLengthInPixels() / getSettings().getNumSubIntervals();
                getGraphics2d()
                        .drawLine(Math.round(pos), -barSize, Math.round(pos), -Math.round(barSize / 2.0f));
            }
        }
    }
}
