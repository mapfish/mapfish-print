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
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.OneOf;
import org.mapfish.print.parser.Requires;

import java.util.Arrays;

/**
 * Parameters relevant to creating Grid layers.
 * CSOFF: VisibilityModifier
 */
public final class GridParam extends AbstractLayerParams {
    private static final int DEFAULT_POINTS_IN_GRID_LINE = 10000;
    private static final int DEFAULT_HALO_RADIUS = 2;
    private static final int DEFAULT_INDENT = 5;
    private static final String DEFAULT_HALO_COLOR = "#FFF";
    private static final String DEFAULT_LABEL_COLOR = "#444";
    private static final String DEFAULT_GRID_COLOR = "gray";

    /**
     * The type of grid to render.  By default it is LINES
     */
    @HasDefaultValue
    public GridType gridType = GridType.LINES;
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
     * If no style is defined then the default grid style will be used.  The default will depend if the type is point or line and will
     * respect {@link #gridColor} and {@link #haloColor} and {@link #haloRadius}.  If {@link #gridType} is {@link GridType#POINTS}
     * then the style will be crosses with a haloRadius sized halo around the cross.  If {@link GridType#LINES} then the style will
     * be a dashed line with no halo.
     * <p/>
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
     * The number of points that will be in the grid line (if the gridType is LINES).  If the line will be curved
     * (for certain projections) then the more points the smoother the curve.
     * <p/>
     * The default number of points is {@value #DEFAULT_POINTS_IN_GRID_LINE}.
     */
    @HasDefaultValue
    public int pointsInLine = DEFAULT_POINTS_IN_GRID_LINE;

    /**
     * The size of the halo around the Grid Labels. The default is {@value #DEFAULT_HALO_RADIUS}.
     */
    @HasDefaultValue
    public int haloRadius = DEFAULT_HALO_RADIUS;
    /**
     * The color of the halo around grid label text. The color is defined the same as colors in CSS. Default is white
     * ({@value #DEFAULT_HALO_COLOR})
     */
    @HasDefaultValue
    public String haloColor = DEFAULT_HALO_COLOR;
    /**
     * The color of the grid label text.  Default is dark gray ({@value #DEFAULT_LABEL_COLOR})
     */
    @HasDefaultValue
    public String labelColor = DEFAULT_LABEL_COLOR;
    /**
     * The color of the grid points or lines.  Default is gray ({@value #DEFAULT_GRID_COLOR})
     */
    @HasDefaultValue
    public String gridColor = DEFAULT_GRID_COLOR;

    /**
     * Configuration for the font of the grid labels.  The default is the default system font.
     */
    @HasDefaultValue
    public GridFontParam font = new GridFontParam();
    /**
     * The number of pixels to indent the grid labels from the end of the map.  The default is {@value #DEFAULT_INDENT}.
     */
    @HasDefaultValue
    public int indent = DEFAULT_INDENT;

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
        Assert.isTrue(this.indent >= 0, "The indent is not permitted to be negative: " + this.indent);
        Assert.isTrue(this.labelColor != null, "labelColor should not be null");
        Assert.isTrue(this.haloColor != null, "haloColor should not be null");
        Assert.isTrue(this.font != null, "font should not be null");
    }

}
