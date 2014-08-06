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
* Draw a bar with alternating black and white zones marking the sub-intervals.
* Intervals have small additional ticks.
*/
public class BarSubScalebarDrawer extends BarScalebarDrawer {

    /**
     * Constructor.
     * @param graphics2d    The graphics context.
     * @param settings      Parameters for rendering the scalebar.
     */
    public BarSubScalebarDrawer(final Graphics2D graphics2d, final ScaleBarRenderSettings settings) {
        super(graphics2d, settings);
    }

    @Override
    //CSOFF: MagicNumber
    protected final void drawBar() {
        super.drawBar();

        for (int i = 0; i <= getParams().intervals; ++i) {
            if (getSettings().getLabels().get(i).getLabelLayout().getCharacterCount() > 0) {
                float pos = i * getSettings().getIntervalLengthInPixels();
                getGraphics2d().drawLine(
                        (int) pos, 0, 
                        (int) pos, (int) (getSettings().getLineWidth() * 1.5f));
            }
        }
    }
    //CSON: MagicNumber
}
