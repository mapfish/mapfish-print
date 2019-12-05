package org.mapfish.print.map.geotools.grid;

import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.OneOf;
import org.mapfish.print.parser.Requires;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.util.Arrays;
import java.util.IllegalFormatException;

/**
 * Parameters relevant to creating Grid layers.
 */
public final class GridParam extends AbstractLayerParams {
    /**
     * Grid label default format pattern for the unit (if valueFormat is used).
     */
    public static final String DEFAULT_UNIT_FORMAT = " %s";
    public static final int DEFAULT_POINTS_IN_GRID_LINE = 10000;
    public static final int DEFAULT_HALO_RADIUS = 1;
    public static final int DEFAULT_INDENT = 5;
    public static final String DEFAULT_HALO_COLOR = "#FFF";
    public static final String DEFAULT_LABEL_COLOR = "#444";
    public static final String DEFAULT_GRID_COLOR = "gray";
    /**
     * The type of grid to render.
     *
     * Can be LINES or POINTS. Default is LINES.
     */
    @HasDefaultValue
    public GridType gridType = GridType.LINES;
    /**
     * The x,y spacing between grid lines.
     *
     * Either {@link #spacing} or {@link #numberOfLines}
     *
     * If spacing is defined then {@link #origin} must also be defined
     */
    @OneOf("spacing")
    @Requires("origin")
    public double[] spacing;

    /**
     * The x,y point of grid origin.
     *
     * This is required if {@link #spacing} is defined.
     */
    @HasDefaultValue
    public double[] origin;

    /**
     * The x,y number of grid lines.
     *
     * The x is the number of lines that run vertically along the page.
     */
    @OneOf("spacing")
    public int[] numberOfLines;
    /**
     * The style name of a style to apply to the features during rendering.  The style name must map to a
     * style in the template or the configuration objects.
     *
     * If no style is defined then the default grid style will be used.  The default will depend if the type
     * is point or line and will respect {@link #gridColor} and {@link #haloColor} and {@link #haloRadius}. If
     * {@link #gridType} is {@link GridType#POINTS} then the style will be crosses with a haloRadius sized
     * halo around the cross.  If {@link GridType#LINES} then the style will be a dashed line with no halo.
     *
     */
    @HasDefaultValue
    public String style;
    /**
     * Indicates if the layer is rendered as SVG.
     *
     * (will default to {@link org.mapfish.print.config.Configuration#defaultToSvg}).
     */
    @HasDefaultValue
    public Boolean renderAsSvg = false;

    /**
     * The number of points that will be in the grid line (if the gridType is LINES).  If the line will be
     * curved (for certain projections) then the more points the smoother the curve.
     *
     * The default number of points is {@value DEFAULT_POINTS_IN_GRID_LINE}.
     */
    @HasDefaultValue
    public int pointsInLine = DEFAULT_POINTS_IN_GRID_LINE;

    /**
     * The size of the halo around the Grid Labels. The default is {@value #DEFAULT_HALO_RADIUS}.
     */
    @HasDefaultValue
    public double haloRadius = DEFAULT_HALO_RADIUS;
    /**
     * The color of the halo around grid label text. The color is defined the same as colors in CSS. Default
     * is white ({@value #DEFAULT_HALO_COLOR})
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
     * The number of pixels to indent the grid labels from the end of the map.  The default is {@value
     * #DEFAULT_INDENT}.
     */
    @HasDefaultValue
    public int indent = DEFAULT_INDENT;
    /**
     * The projection code to use for the labels.  The value should be the string <code>authority:code</code>
     * form identifying the projection.  By default it will be the same projection as the map.
     */
    @HasDefaultValue
    public String labelProjection = null;
    /**
     * The formatting string used to format the label (for example "%1.2f %s"). By default the label is
     * formatted according to the unit and label value. For the format syntax, see
     * <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html">java.util.Formatter</a>.
     * If <code>labelFormat</code> is set, <code>valueFormat</code>, <code>unitFormat</code> and custom
     * separator characters will be ignored.
     */
    @HasDefaultValue
    public String labelFormat = null;
    /**
     * The formatting string used to format the decimal part of a label (for example "###,###"). This
     * parameter is ignored if <code>labelFormat</code> is set. For the format syntax, see
     * <a href="https://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html">DecimalFormat</a>.
     */
    @HasDefaultValue
    public String valueFormat = null;
    /**
     * The formatting string used to format the unit part of a label (for example " %s"). This parameter is
     * ignored if <code>labelFormat</code> is set. <code>valueFormat</code> must be set to use this parameter.
     * For the format syntax, see
     * <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html">java.util.Formatter</a>.
     */
    @HasDefaultValue
    public String unitFormat = null;
    /**
     * The character used to separate the decimal part (for example ","). This parameter is only used if
     * <code>valueFormat</code> is used. The default is the character of the default locale.
     */
    @HasDefaultValue
    public String formatDecimalSeparator = null;
    /**
     * The character used for the thousands separator (for example "'"). This parameter is only used if
     * <code>valueFormat</code> is used. The default is the character of the default locale.
     */
    @HasDefaultValue
    public String formatGroupingSeparator = null;
    /**
     * By default the normal axis order as specified in EPSG code will be used when parsing projections.
     * However the requestor can override this by explicitly declaring that longitude axis is first.
     */
    @HasDefaultValue
    public Boolean longitudeFirst = null;
    /**
     * If true (the default), the labels will be rotated to follow the lines they belong to. Otherwise they
     * are orientated west to east.
     */
    @HasDefaultValue
    public boolean rotateLabels = true;
    /**
     * Apply an X offset to the vertical grid line labels, relative to the vertical grid lines. Defaults to
     * 0.
     */
    @HasDefaultValue
    public double verticalXOffset = 0;
    /**
     * Apply an Y offset to horizontal grid line labels, relative to the horizontal grid lines. Defaults to
     * 0.
     */
    @HasDefaultValue
    public double horizontalYOffset = 0;
    private GridLabelFormat gridLabelFormat = null;
    private CoordinateReferenceSystem labelCRS;

    /**
     * Initialize default values and validate that config is correct.
     */
    public void postConstruct() {
        Assert.isTrue(this.spacing == null || this.spacing.length == 2,
                      GridLayer.class.getSimpleName() +
                              ".spacing has the wrong number of elements.  Expected 2 (x,y) but was: " +
                              Arrays.toString(this.spacing));
        Assert.isTrue(this.numberOfLines == null || this.numberOfLines.length == 2,
                      GridLayer.class.getSimpleName() +
                              ".numberOfLines has the wrong number of elements.  Expected 2 (x,y) but was: " +
                              Arrays.toString(this.numberOfLines));
        Assert.isTrue(this.pointsInLine > 2,
                      "There must be at least 2 points in a line.  There were: " + this.pointsInLine);
        Assert.isTrue(this.indent >= 0, "The indent is not permitted to be negative: " + this.indent);
        Assert.isTrue(this.labelColor != null, "labelColor should not be null");
        Assert.isTrue(this.haloColor != null, "haloColor should not be null");
        Assert.isTrue(this.font != null, "font should not be null");

        try {
            if (this.labelProjection != null) {
                if (this.longitudeFirst != null) {
                    this.labelCRS = CRS.decode(this.labelProjection, this.longitudeFirst);
                } else {
                    this.labelCRS = CRS.decode(this.labelProjection);
                }
            }
        } catch (FactoryException e) {
            throw new IllegalArgumentException("The projection code: " + this.labelProjection +
                                                       " is not valid. Error message when parsing code: " +
                                                       e.getMessage());
        }
        if (this.labelFormat != null || this.valueFormat != null || this.unitFormat != null) {
            GridLabelFormat format = GridLabelFormat.fromConfig(this);
            if (format == null) {
                throw new IllegalArgumentException("`labelFormat` or `valueFormat` must be set");
            }
            try {
                format.format(2.0, "m");
            } catch (IllegalFormatException e) {
                throw new IllegalArgumentException("Invalid label format");
            }
            this.gridLabelFormat = format;
        }
    }

    /**
     * Determine which unit to use when creating grid labels.
     *
     * @param mapCrs the crs of the map, used if the {@link #labelProjection} is not defined.
     */
    public String calculateLabelUnit(final CoordinateReferenceSystem mapCrs) {
        String unit;
        if (this.labelProjection != null) {
            unit = this.labelCRS.getCoordinateSystem().getAxis(0).getUnit().toString();
        } else {
            unit = mapCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
        }

        return unit;
    }

    /**
     * Determine which math transform to use when creating the coordinate of the label.
     *
     * @param mapCrs the crs of the map, used if the {@link #labelProjection} is not defined.
     */
    public MathTransform calculateLabelTransform(final CoordinateReferenceSystem mapCrs) {
        MathTransform labelTransform;
        if (this.labelProjection != null) {
            try {
                labelTransform = CRS.findMathTransform(mapCrs, this.labelCRS, true);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
        } else {
            labelTransform = IdentityTransform.create(2);
        }

        return labelTransform;
    }

    public GridLabelFormat getGridLabelFormat() {
        return this.gridLabelFormat;
    }
}
