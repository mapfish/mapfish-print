package org.mapfish.print.attribute;


import org.apache.commons.lang3.StringUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.processor.map.scalebar.HorizontalAlign;
import org.mapfish.print.processor.map.scalebar.Orientation;
import org.mapfish.print.processor.map.scalebar.Type;
import org.mapfish.print.processor.map.scalebar.VerticalAlign;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

/**
 * <p>The attributes for {@link org.mapfish.print.processor.map.scalebar.CreateScalebarProcessor} (see
 * <a href="processors.html#!createScalebar">!createScalebar</a> processor).</p>
 * [[examples=verboseExample,print_osm_new_york_EPSG_3857,print_osm_new_york_nosubreports]]
 */
public class ScalebarAttribute extends ReflectiveAttribute<ScalebarAttribute.ScalebarAttributeValues> {

    private Integer width = null;
    private Integer height = null;
    private Boolean createSubReport = true;

    @Override
    public final void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.width == null || this.width < 1) {
            validationErrors.add(new ConfigurationException(
                    "width field is not legal: " + this.width + " in " + getClass().getName()));
        }

        if (this.height == null || this.height < 1) {
            validationErrors.add(new ConfigurationException(
                    "height field is not legal: " + this.height + " in " + getClass().getName()));
        }
    }

    @Override
    public final ScalebarAttributeValues createValue(final Template template) {
        return new ScalebarAttributeValues(new Dimension(this.width, this.height), this.createSubReport);
    }

    @Override
    public final Class<? extends ScalebarAttributeValues> getValueType() {
        return ScalebarAttributeValues.class;
    }

    public final Integer getWidth() {
        return this.width;
    }

    /**
     * The width of the scalebar in pixels. This value should match the width of the sub-report in the
     * JasperReport template.
     *
     * @param width Width
     */
    public final void setWidth(final Integer width) {
        this.width = width;
    }

    public final Integer getHeight() {
        return this.height;
    }

    /**
     * The height of the scalebar in pixels. This value should match the height of the sub-report in the
     * JasperReport template.
     *
     * @param height Height
     */
    public final void setHeight(final Integer height) {
        this.height = height;
    }


    public final Boolean getCreateSubReport() {
        return this.createSubReport;
    }

    /**
     * Specifies whether a subreport should be created, or only a graphic.
     *
     * @param createSubReport Create a sub-report?
     */
    public final void setCreateSubReport(final Boolean createSubReport) {
        this.createSubReport = createSubReport;
    }

    /**
     * The value of {@link ScalebarAttribute}.
     */
    public class ScalebarAttributeValues {

        private static final int DEFAULT_INTERVALS = 3;
        private static final String DEFAULT_FONT = "Helvetica";
        private static final int DEFAULT_FONT_SIZE = 12;
        private static final String DEFAULT_FONT_COLOR = "black";
        private static final String DEFAULT_COLOR = "black";
        private static final String DEFAULT_BAR_BG_COLOR = "white";
        private static final String DEFAULT_BACKGROUND_COLOR = "rgba(255, 255, 255, 0)";

        private final Dimension size;
        private final boolean createSubReport;

        /**
         * The scalebar type.
         *
         * <p>Available types:</p>
         * <ul>
         * <li>"line": A simple line with graduations.</li>
         * <li>"bar" (default): A thick bar with alternating black and white zones marking the intervals.
         * The colors can be customized by changing the properties `color` and `barBgColor`.
         * </li>
         * <li>"bar_sub": Like "bar", but with little ticks for the labels.</li>
         * </ul>
         */
        @HasDefaultValue
        public String type = Type.BAR.getLabel();

        /**
         * The unit to use.
         *
         * <p>The unit can be any of:</p>
         * <ul>
         * <li>m (mm, cm, m or km)</li>
         * <li>ft (in, ft, yd, mi)</li>
         * <li>degrees (min, sec, °)</li>
         * </ul>
         *
         * <p>If the value is too big or too small, the module will switch to one of the unit in parenthesis
         * (the same unit is used for every interval). If this behaviour is not desired, the `lockUnits`
         * parameter will force the declared unit (or map unit if no unit is declared) to be used for the
         * scalebar.</p>
         */
        @HasDefaultValue
        public String unit = null;

        /**
         * Use geodetic measurement calculations for the scalebar.
         */
        @HasDefaultValue
        public boolean geodetic = false;

        /**
         * Force that the given unit is used (default: false). For example if the unit is set to meters and
         * `lockUnits` is enabled, then meters is always used, even when kilometers would create nicer
         * values.
         */
        @HasDefaultValue
        public Boolean lockUnits = false;

        /**
         * The number of intervals (default: 3). There must be at least two intervals.
         */
        @HasDefaultValue
        public Integer intervals = DEFAULT_INTERVALS;

        /**
         * Should sub-intervals be shown? Default: false
         * <p>The main intervals are divided into additional sub-intervals to provide
         * visual guidance. The number of sub-intervals depends on the length of an interval.</p>
         */
        @HasDefaultValue
        public Boolean subIntervals = false;

        /**
         * The thickness of the bar or the height of the tick marks on the line (in pixel).
         */
        @HasDefaultValue
        public Integer barSize = null;

        /**
         * The thickness of the lines or the bar border (in pixel).
         */
        @HasDefaultValue
        public Integer lineWidth = null;

        /**
         * The distance between scalebar and labels (in pixel).
         */
        @HasDefaultValue
        public Integer labelDistance = null;

        /**
         * The padding around the scalebar (in pixel).
         */
        @HasDefaultValue
        public Integer padding = null;

        /**
         * The font used for the labels (default: Helvetica).
         */
        @HasDefaultValue
        public String font = DEFAULT_FONT;

        /**
         * The font size (in pt) of the labels (default: 12).
         */
        @HasDefaultValue
        public Integer fontSize = DEFAULT_FONT_SIZE;

        /**
         * The font color of the labels (default: black).
         */
        @HasDefaultValue
        public String fontColor = DEFAULT_FONT_COLOR;

        /**
         * The color used to draw the bar and lines (default: black).
         */
        @HasDefaultValue
        public String color = DEFAULT_COLOR;

        /**
         * The color used to draw the alternating blocks for style "bar" and "bar_sub" (default: white).
         */
        @HasDefaultValue
        public String barBgColor = DEFAULT_BAR_BG_COLOR;

        /**
         * The background color for the scalebar graphic (default: rgba(255, 255, 255, 0)).
         */
        @HasDefaultValue
        public String backgroundColor = DEFAULT_BACKGROUND_COLOR;

        /**
         * The scalebar orientation.
         *
         * <p>Available options:</p>
         * <ul>
         * <li>"horizontalLabelsBelow" (default): Horizontal scalebar and the labels are shown below the
         * bar.</li>
         * <li>"horizontalLabelsAbove": Horizontal scalebar and the labels are shown above the bar.</li>
         * <li>"verticalLabelsLeft": Vertical scalebar and the labels are shown left of the bar.</li>
         * <li>"verticalLabelsRight": Vertical scalebar and the labels are shown right of the bar.</li>
         * </ul>
         */
        @HasDefaultValue
        public String orientation = Orientation.HORIZONTAL_LABELS_BELOW.getLabel();

        /**
         * Rotate the label to some degree.
         */
        @HasDefaultValue
        public float labelRotation = 0.0f;

        /**
         * The horizontal alignment of the scalebar inside the scalebar graphic (default: left).
         */
        @HasDefaultValue
        public String align = HorizontalAlign.LEFT.getLabel();

        /**
         * The vertical alignment of the scalebar inside the scalebar graphic (default: bottom).
         */
        @HasDefaultValue
        public String verticalAlign = VerticalAlign.BOTTOM.getLabel();

        /**
         * Indicates if the scalebar graphic is rendered as SVG (will default to {@link
         * org.mapfish.print.config.Configuration#defaultToSvg}).
         */
        @HasDefaultValue
        public Boolean renderAsSvg = true;

        /**
         * Constructor.
         *
         * @param size The size of the scalebar graphic in the Jasper report (in pixels).
         * @param createSubReport Create a sub-report?
         */
        public ScalebarAttributeValues(final Dimension size, final boolean createSubReport) {
            this.size = size;
            this.createSubReport = createSubReport;
        }

        /**
         * Initialize default values and validate that the config is correct.
         */
        public final void postConstruct() {
            if (getType() == null) {
                throw new IllegalArgumentException("invalid scalebar type: " + this.type);
            }
            if (this.unit != null && DistanceUnit.fromString(this.unit) == null) {
                throw new IllegalArgumentException("invalid unit: " + this.unit);
            }
            if (this.intervals < 1) {
                throw new IllegalArgumentException("invalid number of intervals: " + this.intervals);
            }
            if (this.color != null && !ColorParser.canParseColor(this.color)) {
                throw new IllegalArgumentException("invalid color: " + this.color);
            }
            if (this.fontColor != null && !ColorParser.canParseColor(this.fontColor)) {
                throw new IllegalArgumentException("invalid font color: " + this.fontColor);
            }
            if (this.barBgColor != null && !ColorParser.canParseColor(this.barBgColor)) {
                throw new IllegalArgumentException("invalid bar background color: " + this.barBgColor);
            }
            if (this.backgroundColor != null && !ColorParser.canParseColor(this.backgroundColor)) {
                throw new IllegalArgumentException("invalid background color: " + this.backgroundColor);
            }
            if (getOrientation() == null) {
                throw new IllegalArgumentException("invalid scalebar orientation: " + this.orientation);
            }
            if (getAlign() == null) {
                throw new IllegalArgumentException("invalid align: " + this.align);
            }
            if (getVerticalAlign() == null) {
                throw new IllegalArgumentException("invalid verticalAlign: " + this.verticalAlign);
            }
        }

        public final Dimension getSize() {
            return this.size;
        }

        public final Color getColor() {
            return ColorParser.toColor(this.color);
        }

        public final Color getFontColor() {
            return ColorParser.toColor(this.fontColor);
        }

        public final Color getBarBgColor() {
            return ColorParser.toColor(this.barBgColor);
        }

        public final Color getBackgroundColor() {
            return ColorParser.toColor(this.backgroundColor);
        }

        /**
         * @return Return the scalebar type.
         */
        public final Type getType() {
            if (StringUtils.isEmpty(this.type)) {
                return null;
            } else {
                return Type.fromString(this.type);
            }
        }

        /**
         * @return Return the unit to use for the scalebar.
         */
        public final DistanceUnit getUnit() {
            if (StringUtils.isEmpty(this.unit)) {
                return null;
            } else {
                return DistanceUnit.fromString(this.unit);
            }
        }

        /**
         * @return Return the scalebar orientation.
         */
        public final Orientation getOrientation() {
            if (StringUtils.isEmpty(this.orientation)) {
                return null;
            } else {
                return Orientation.fromString(this.orientation);
            }
        }

        /**
         * @return Return the label rotation
         */
        public final float getLabelRotation() {
            return (float) Math.toRadians(this.labelRotation);
        }

        /**
         * @return Return the horizontal alignment.
         */
        public final HorizontalAlign getAlign() {
            if (StringUtils.isEmpty(this.align)) {
                return null;
            } else {
                return HorizontalAlign.fromString(this.align);
            }
        }

        /**
         * @return Return the vertical alignment.
         */
        public final VerticalAlign getVerticalAlign() {
            if (StringUtils.isEmpty(this.verticalAlign)) {
                return null;
            } else {
                return VerticalAlign.fromString(this.verticalAlign);
            }
        }

        public final boolean isCreateSubReport() {
            return this.createSubReport;
        }
    }
}
