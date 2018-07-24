package org.mapfish.print.attribute;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;

import java.net.URL;
import java.util.List;

/**
 * Objects needed by the {@link org.mapfish.print.processor.jasper.LegendProcessor} (see
 * <a href="processors.html#!prepareLegend">!prepareLegend</a> processor).
 * [[examples=verboseExample,legend_cropped]]
 */
public final class LegendAttribute extends ReflectiveAttribute<LegendAttribute.LegendAttributeValue> {

    @Override
    public Class<LegendAttributeValue> getValueType() {
        return LegendAttributeValue.class;
    }

    @Override
    public LegendAttributeValue createValue(final Template template) {
        return new LegendAttributeValue();
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }

    /**
     * The data required to render a map legend.
     */
    public static final class LegendAttributeValue {

        /**
         * Name of the legend class.
         */
        @HasDefaultValue
        public String name;

        /**
         * DPI of the legend icons.
         */
        @HasDefaultValue
        public Double dpi;

        /**
         * Urls for downloading icons for each legend row.
         */
        @HasDefaultValue
        public URL[] icons;

        /**
         * Other embedded classes.
         */
        @HasDefaultValue
        public LegendAttributeValue[] classes;
    }
}
