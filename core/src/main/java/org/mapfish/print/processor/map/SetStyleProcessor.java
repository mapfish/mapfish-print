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

package org.mapfish.print.processor.map;

import org.mapfish.print.attribute.StyleAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.processor.AbstractProcessor;

/**
 * Processor to set features to the vector layers.
 * <p/>
 * Created by Stéphane Brunner on 24/4/14.
 */
public class SetStyleProcessor extends
        AbstractProcessor<SetStyleProcessor.Input, SetStyleProcessor.Output> {

    /**
     * Constructor.
     */
    protected SetStyleProcessor() {
        super(Output.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values) throws Exception {
        for (MapLayer layer : values.map.getLayers()) {
            if (layer instanceof AbstractFeatureSourceLayer) {
                ((AbstractFeatureSourceLayer) layer).setStyle(values.style.getStyle());
            }
        }

        return new Output(values.map);
    }

    /**
     * The input parameter object for {@link SetFeaturesProcessor}.
     */
    public static final class Input {
        /**
         * The map to update.
         */
        public MapAttributeValues map;

        /**
         * The features.
         */
        public StyleAttribute.StylesAttributeValues style;
    }

    /**
     * The Output object of the processor method.
     */
    public static final class Output {
        /**
         * The input map.
         */
        public MapAttributeValues map;

        /**
         * Construct.
         * @param map The input map
         */
        private Output(final MapAttributeValues map) {
            this.map = map;
        }
    }
}
