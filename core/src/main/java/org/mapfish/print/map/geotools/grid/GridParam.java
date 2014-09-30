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

package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.Constants;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.OneOf;
import org.mapfish.print.parser.Requires;

import java.util.Arrays;

/**
 * Parameters relevant to creating Grid layers.
 * CSOFF: VisibilityModifier
 */
public final class GridParam {
    private static final int DEFAULT_POINTS_IN_GRID_LINE = 10000;
    /**
     * The x,y spacing between grid lines.
     * <p/>
     * Either {@link #spacing} or {@link #numberOfLines}
     * <p/>
     * If spacing is defined then {@link #origin} must also be defined
     */
    @OneOf("spacing")
    @Requires("origin")
    public double[] spacing;

    /**
     * The x,y point of grid origin.
     * <p/>
     * This is required if {@link #spacing} is defined.
     */
    @HasDefaultValue
    public double[] origin;

    /**
     * The x,y number of grid lines.
     * <p/>
     * The x is the number of lines that run vertically along the page.
     */
    @OneOf("spacing")
    public int[] numberOfLines;
    /**
     * The style name of a style to apply to the features during rendering.  The style name must map to a style in the
     * template or the configuration objects.
     * <p/>
     * If no style is defined then the default grid style will be used.
     * <p/>
     * The feature for the grid will have a line geometry and will have the following attributes:
     * <ul>
     *     <li>
     *         {@value org.mapfish.print.Constants.Style.Grid#ATT_ROTATION} -- the rotation for a label that is perpendicular
     *         to the line
     *     </li>
     *     <li>
     *         {@value org.mapfish.print.Constants.Style.Grid#ATT_LABEL} -- The suggested text of the label
     *     </li>
     *     <li>
     *         {@value org.mapfish.print.Constants.Style.Grid#ATT_X_DISPLACEMENT} -- The x-displacement of one of the labels (might
     *         be top or left) the unit is pixels.
     *     </li>
     *     <li>
     *         {@value org.mapfish.print.Constants.Style.Grid#ATT_Y_DISPLACEMENT} -- The y-displacement of one of the labels (might
     *         be top or left) the unit is pixels.
     *     </li>
     * </ul>
     */
    @HasDefaultValue
    public String style;
    /**
     * Indicates if the layer is rendered as SVG.
     * <p/>
     * (will default to {@link org.mapfish.print.config.Configuration#defaultStyle}).
     */
    @HasDefaultValue
    public Boolean renderAsSvg = false;

    /**
     * The number of points that will be in the grid line.  If the line will be curved (for certain projections) then the more
     * points the smoother the curve.
     * <p/>
     * The default number of points is {@value #DEFAULT_POINTS_IN_GRID_LINE}.
     */
    @HasDefaultValue
    public int pointsInLine = DEFAULT_POINTS_IN_GRID_LINE;

    /**
     * Initialize default values and validate that config is correct.
     */
    public void postConstruct() {
        Assert.isTrue(this.spacing == null || this.spacing.length == 2,
                GridLayer.class.getSimpleName() + ".spacing has the wrong number of elements.  Expected 2 (x,y) but was: " +
                Arrays.toString(this.spacing));
        Assert.isTrue(this.numberOfLines == null || this.numberOfLines.length == 2,
                GridLayer.class.getSimpleName() + ".numberOfLines has the wrong number of elements.  Expected 2 (x,y) but was: " +
                Arrays.toString(this.numberOfLines));
        Assert.isTrue(this.pointsInLine > 2, "There must be at least 2 points in a line.  There were: " + this.pointsInLine);
        if (this.style == null) {
            this.style = Constants.Style.Grid.NAME;
        }
    }
}
