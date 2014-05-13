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

package org.mapfish.print.attribute;

import org.geotools.styling.Style;
import org.mapfish.print.attribute.StyleAttribute.StylesAttributeValues;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.style.StringSLDParserPlugin;
import org.mapfish.print.map.style.StyleParserPlugin;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Attribute for GeoJson Styles collection.
 * <p/>
 * Created by Stéphane Brunner on 24/4/14.
 */
public class StyleAttribute extends ReflectiveAttribute<StylesAttributeValues> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleAttribute.class);

    @Override
    public final StylesAttributeValues createValue(final Template template) {
        StylesAttributeValues result = new StylesAttributeValues();
        return result;
    }

    @Override
    public void validate(final List<Throwable> validationErrors) {
        // no validation needed.
    }

    /**
     * The value of {@link StyleAttribute}.
     */
    public final class StylesAttributeValues {
        /**
         * The SDL string.
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
         */
        public void postConstruct() throws FactoryException {
            final StyleParserPlugin parser = new StringSLDParserPlugin();
            try {
                this.styleObject = parser.parseStyle(null, this.style).get();
            } catch (Throwable e) {
                throw new RuntimeException("Unable to parce the SDL style", e);
            }
        }

        /**
         * Validate the values provided by the request data and construct MapBounds and parse the layers.
         */
        public Style getStyle() throws FactoryException {
            return this.styleObject;

        }
    }
}
