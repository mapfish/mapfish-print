/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.processor.map.scalebar;

import java.awt.Graphics2D;

/**
 * Draw a simple line with ticks.
 */
public class LineScalebarDrawer extends ScalebarDrawer {

    /**
     * Constructor.
     * @param graphics2d    The graphics context.
     * @param settings      Parameters for rendering the scalebar.
     */
    public LineScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        super(graphics2d, settings);
    }

    @Override
    protected final void drawBar() {
        final int barSize = getSettings().getBarSize();

        // first tick
        getGraphics2d().drawLine(0, 0, 0, -barSize);

        // horizontal line
        int intervalsLength = (int) (getSettings().getIntervalLengthInPixels() * getParams().intervals);
        getGraphics2d().drawLine(0, -barSize, intervalsLength, -barSize);

        // last tick
        getGraphics2d().drawLine(intervalsLength, -barSize, intervalsLength, 0);

        // draw the ticks for each interval
        for (int i = 0; i < getParams().intervals; i++) {
            float pos = i * getSettings().getIntervalLengthInPixels();
            if (i > 0) {
                getGraphics2d().drawLine((int) pos, 0, (int) pos, -barSize);
            }
            // draw the ticks for the sub-intervals
            for (int j = 1; j < getSettings().getNumSubIntervals(); j++) {
                pos += getSettings().getIntervalLengthInPixels() / getSettings().getNumSubIntervals();
                getGraphics2d().drawLine((int) pos, -barSize, (int) pos, (int) -(barSize / 2.0f));
            }
        }
    }
}
