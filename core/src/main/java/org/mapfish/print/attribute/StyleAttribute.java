package org.mapfish.print.attribute;

import org.geotools.styling.Style;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.StyleAttribute.StylesAttributeValues;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.style.SLDParserPlugin;
import org.mapfish.print.map.style.StyleParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Attribute for GeoJson Styles collection.
 */
public final class StyleAttribute extends ReflectiveAttribute<StylesAttributeValues> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleAttribute.class);

    @Override
    protected Class<StylesAttributeValues> getValueType() {
        return StylesAttributeValues.class;
    }

    @Override
    public StylesAttributeValues createValue(final Template template) {
        StylesAttributeValues result = new StylesAttributeValues();
        return result;
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
         * The SLD string.
         */
        public String style;

        /**
         * The Style.
         */
        private Style styleObject;

        /**
         * Constructor.
         */
        public StylesAttributeValues() {
        }

        /**
         * Validate the values provided by the request data and construct MapBounds and parse the layers.
         * @param clientHttpRequestFactory a factory for creating http requests
         * @param mapContext information about the map projection, bounds, size, etc...
         */
        public synchronized Style getStyle(@Nonnull final MfClientHttpRequestFactory clientHttpRequestFactory,
                                           @Nonnull final MapfishMapContext mapContext) throws Exception {
            if (this.styleObject == null && this.style != null) {
                final StyleParserPlugin parser = new SLDParserPlugin();
                try {
                    this.styleObject = parser.parseStyle(null, clientHttpRequestFactory, this.style, mapContext).get();
                } catch (Throwable throwable) {
                    throw ExceptionUtils.getRuntimeException(throwable);
                }
            }
            return this.styleObject;

        }
    }
}
