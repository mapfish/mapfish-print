package org.mapfish.print.attribute;

import org.mapfish.print.attribute.StyleAttribute.StylesAttributeValues;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Attribute for GeoJson Styles collection.
 * [[examples=report]]
 */
public final class StyleAttribute extends ReflectiveAttribute<StylesAttributeValues> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleAttribute.class);

    @Override
    public Class<StylesAttributeValues> getValueType() {
        return StylesAttributeValues.class;
    }

    @Override
    public StylesAttributeValues createValue(final Template template) {
        return new StylesAttributeValues();
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no validation needed.
    }

    /**
     * The value of {@link StyleAttribute}.
     */
    public static final class StylesAttributeValues {
        /**
         * The style string.
         */
        public String style;
    }
}
