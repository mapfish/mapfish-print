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

import java.awt.Color;
import java.awt.Graphics2D;

/**
* Draw a bar with alternating black and white zones marking the sub-intervals.
*/
public class BarScalebarDrawer extends ScalebarDrawer {

    /**
     * Constructor.
     * @param graphics2d    The graphics context.
     * @param settings      Parameters for rendering the scalebar.
     */
    public BarScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        super(graphics2d, settings);
    }

    //CSOFF: DesignForExtension
    @Override
    protected void drawBar() {
    //CSON: DesignForExtension
        final int barSize = getSettings().getBarSize();

        float subIntervalWidth = getSettings().getIntervalLengthInPixels() / getSettings().getNumSubIntervals();
        int intervalsTotal = getParams().intervals * getSettings().getNumSubIntervals();
        for (int i = 0; i < intervalsTotal; i++) {
            float pos = i * subIntervalWidth;
            final Color color = i % 2 == 0 ? getParams().getBarBgColor() : getParams().getColor();
            if (color != null) {
                getGraphics2d().setColor(color);
                getGraphics2d().fillRect((int) pos, -barSize, (int) subIntervalWidth, barSize);
            }
        }

        getGraphics2d().setColor(getParams().getColor());
        getGraphics2d().drawRect(
                0, -barSize,
                (int) (getSettings().getIntervalLengthInPixels() * getParams().intervals), barSize);
    }
}
