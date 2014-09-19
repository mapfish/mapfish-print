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
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;

import java.net.URL;
import java.util.List;

/**
 * Objects needed by the {@link org.mapfish.print.processor.jasper.LegendProcessor}.
 */
public final class LegendAttribute extends ReflectiveAttribute<LegendAttribute.LegendAttributeValue> {

    @Override
    protected Class<LegendAttributeValue> getValueType() {
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
     *
     * @author Jesse
     */
    public static final class LegendAttributeValue {

        /**
         * Name of the legend class.
         */
        @HasDefaultValue
        public String name;

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
