package org.mapfish.print.attribute;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.parser.HasDefaultValue;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

/**
 * <p>The attributes for {@link org.mapfish.print.processor.map.CreateNorthArrowProcessor} (see
 * <a href="processors.html#!createNorthArrow">!createNorthArrow</a> processor).</p>
 * [[examples=verboseExample,print_osm_new_york_nosubreports]]
 */
public class NorthArrowAttribute extends ReflectiveAttribute<NorthArrowAttribute.NorthArrowAttributeValues> {

    private Integer size = null;
    private Boolean createSubReport = true;

    @Override
    public final void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.size == null || this.size < 1) {
            validationErrors.add(new ConfigurationException(
                    "size field is not legal: " + this.size + " in " + getClass().getName()));
        }
    }

    @Override
    public final NorthArrowAttributeValues createValue(final Template template) {
        return new NorthArrowAttributeValues(new Dimension(this.size, this.size), this.createSubReport);
    }

    @Override
    public final Class<? extends NorthArrowAttributeValues> getValueType() {
        return NorthArrowAttributeValues.class;
    }

    public final Integer getSize() {
        return this.size;
    }

    /**
     * The size (width and height) of the north-arrow graphic in the JasperReport template.
     * <p>The graphic is a square so that the arrow can be rotated properly.</p>
     *
     * @param size The size (width and height).
     */
    public final void setSize(final Integer size) {
        this.size = size;
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
     * The value of {@link NorthArrowAttribute}.
     */
    public class NorthArrowAttributeValues {

        private static final String DEFAULT_BACKGROUND_COLOR = "rgba(255, 255, 255, 0)";

        private final Dimension size;
        private final boolean createSubReport;

        /**
         * The path to a graphic to use for the north-arrow.
         * <p>It can either be an URL ("http://xyx.com/img/north-arrow.png") or
         * a file in the configuration folder ("file://NorthArrow.svg"). Both SVG graphics and raster graphics
         * (png, jpeg, tiff, ...) are supported.</p>
         * <p>While the resulting graphic used in the JasperReport template is
         * a square, this graphic can have an arbitrary aspect ratio. The graphic will be scaled to the output
         * size and rotated around its center.</p>
         * <p>If no graphic is given, a default north-arrow is used.</p>
         */
        @HasDefaultValue
        public String graphic = null;

        /**
         * The background color for the north-arrow graphic (default: rgba(255, 255, 255, 0)).
         */
        @HasDefaultValue
        public String backgroundColor = DEFAULT_BACKGROUND_COLOR;

        /**
         * Constructor.
         *
         * @param size The size of the scalebar graphic in the Jasper report (in pixels).
         * @param createSubReport Create a sub-report?
         */
        public NorthArrowAttributeValues(final Dimension size, final boolean createSubReport) {
            this.size = size;
            this.createSubReport = createSubReport;
        }

        /**
         * Initialize default values and validate that the config is correct.
         */
        public final void postConstruct() {
            if (this.backgroundColor != null && !ColorParser.canParseColor(this.backgroundColor)) {
                throw new IllegalArgumentException("invalid background color: " + this.backgroundColor);
            }
        }

        public final Dimension getSize() {
            return this.size;
        }

        public final String getGraphic() {
            return this.graphic;
        }

        public final Color getBackgroundColor() {
            return ColorParser.toColor(this.backgroundColor);
        }

        public final boolean isCreateSubReport() {
            return this.createSubReport;
        }

    }
}
