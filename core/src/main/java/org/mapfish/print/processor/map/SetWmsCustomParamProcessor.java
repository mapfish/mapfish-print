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

import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.map.image.wms.WmsLayer;
import org.mapfish.print.processor.AbstractProcessor;

/**
 * Processor to set a param to the WMS layers.
 * <p/>
 * Created by Stéphane Brunner on 16/4/14.
 */
public class SetWmsCustomParamProcessor extends AbstractProcessor<SetWmsCustomParamProcessor.Input, SetWmsCustomParamProcessor.Output> {

    /**
     * The parameter name.
     */
    private String paramName;


    /**
     * Constructor.
     */
    protected SetWmsCustomParamProcessor() {
        super(Output.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values) throws Exception {
        for (MapLayer layer : values.map.getLayers()) {
            if (layer instanceof WmsLayer) {
                ((WmsLayer) layer).getParams().setCustomParam(this.paramName, values.value);
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
         * The value.
         */
        public String value;
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

    /**
     * Set the parameter name.
     * @param paramName the parameter name
     */
    public final void setParamName(final String paramName) {
        this.paramName = paramName;
    }
}
