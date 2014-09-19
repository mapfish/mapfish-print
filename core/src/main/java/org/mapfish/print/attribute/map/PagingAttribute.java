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

package org.mapfish.print.attribute.map;

import org.mapfish.print.attribute.ReflectiveAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;

import java.util.List;

/**
 * Attribute that defines how a map is displayed across many pages.
 *
 * <p>
 *     This is used by the <a href="#/processors#!paging">Paging Processor</a>.
 * </p>
 *
 * @author Jesse on 8/27/2014.
 */
public final class PagingAttribute extends ReflectiveAttribute<PagingAttribute.PagingProcessorValues> {

    @Override
    protected Class<? extends PagingProcessorValues> getValueType() {
        return PagingProcessorValues.class;
    }

    @Override
    public PagingProcessorValues createValue(final Template template) {
        return new PagingProcessorValues();
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // nothing to be done
    }

    /**
     * Values object for this attribute type.
     */
    public static class PagingProcessorValues {
        /**
         * The scale denominator for each page/sub-map.
         */
        public double scale;
        /**
         * The amount that each page/sub-map should overlap each other.
         * <p>
         *     For example if the value is 1 and the projection of the map is degrees then the overlap will be 1 degree.
         * </p>
         * <p>
         *     The default is to not have any overlap.
         * </p>
         */
        @HasDefaultValue
        public double overlap = 0;

        /**
         * Indicates how to render the area of interest on this sub-map. This
         * makes it easier to how the all the sub-maps fit together to for the complete map.  Also if the map is rendered as a whole
         * in one part of the report one can easily see where in the complete map the sub-map fits, even without looking at the
         * labelling.
         * <p>
         *     For options see: {@link org.mapfish.print.attribute.map.AreaOfInterest.AoiDisplay}
         * </p>
         * <p>
         *     By default the rendering in the <a href="#/attributes#!map">map attribute's</a> area of interest will be used
         * </p>
         */
        @HasDefaultValue
        public AreaOfInterest.AoiDisplay aoiDisplay;


        /**
         * If this is defined it will override the style used for rendering the Area Of Interest in the
         * main <a href="#/attributes#!map">map attribute's</a> Area of Interest definition.
         */
        @HasDefaultValue
        public String aoiStyle = null;


    }
}
