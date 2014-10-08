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

import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.parser.HasDefaultValue;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

/**
 * The attributes for {@link org.mapfish.print.processor.map.scalebar.CreateNorthArrowProcessor}.
 */
public class NorthArrowAttribute extends ReflectiveAttribute<NorthArrowAttribute.NorthArrowAttributeValues> {

    private Integer size = null;

    @Override
    public final void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.size == null || this.size < 1) {
            validationErrors.add(new ConfigurationException("size field is not legal: " + this.size + " in " + getClass().getName()));
        }
    }

    @Override
    public final NorthArrowAttributeValues createValue(final Template template) {
        return new NorthArrowAttributeValues(new Dimension(this.size, this.size));
    }

    @Override
    protected final Class<? extends NorthArrowAttributeValues> getValueType() {
        return NorthArrowAttributeValues.class;
    }

    public final Integer getSize() {
        return this.size;
    }

    /**
     * The size (width and height) of the north-arrow graphic in the
     * JasperReport template.
     * <p>The graphic is a square so that the arrow can be rotated properly.</p>
     * @param size The size (width and height).
     */
    public final void setSize(final Integer size) {
        this.size = size;
    }

    /**
     * The value of {@link NorthArrowAttribute}.
     */
    public class NorthArrowAttributeValues {

        private static final String DEFAULT_BACKGROUND_COLOR = "rgba(255, 255, 255, 0)";

        private final Dimension size;

        /**
         * The path to a graphic to use for the north-arrow.
         * <p>It can either be an URL ("http://xyx.com/img/north-arrow.png") or
         * a file in the configuration folder ("file://NorthArrow.svg").
         * Both SVG graphics and raster graphics (png, jpeg, tiff, ...) are
         * supported.</p>
         * <p>While the resulting graphic used in the JasperReport template is
         * a square, this graphic can have an arbitrary aspect ratio. The graphic
         * will be scaled to the output size and rotated around its center.</p>
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
         */
        public NorthArrowAttributeValues(final Dimension size) {
            this.size = size;
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
    }
}
