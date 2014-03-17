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

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.json.PJsonObject;

/**
 * The attributes for {@link org.mapfish.print.processor.map.MapProcessor}.
 */
public class MapAttribute extends AbstractAttribute<MapAttribute.MapAttributeValues> {

    static final String MAX_DPI = "maxDpi";
    static final String WIDTH = "width";
    static final String HEIGHT = "height";

    private float maxDpi;
    private float width;
    private float height;

    @Override
    public MapAttributeValues getValue(final PJsonObject values, final String name) {
        return new MapAttributeValues(values.getJSONObject(name));
    }

    @Override
    protected String getType() {
        return "map";
    }

    @Override
    protected void additionalPrintClientConfig(final JSONWriter json) throws JSONException {
        json.key(MAX_DPI).value(this.maxDpi);
        json.key(WIDTH).value(this.width);
        json.key(HEIGHT).value(this.height);
    }

    public void setMaxDpi(final float maxDpi) {
        this.maxDpi = maxDpi;
    }

    public void setWidth(final float width) {
        this.width = width;
    }

    public void setHeight(final float height) {
        this.height = height;
    }

    /**
     * The value of {@link org.mapfish.print.attribute.MapAttribute}.
     */
    public static class MapAttributeValues {

        private final double maxDPI;
        private final int width;
        private final int height;

        /**
         * Constructor.
         * @param jsonObject json containing attribute information.
         */
        public MapAttributeValues(final PJsonObject jsonObject) {
            this.maxDPI = jsonObject.getDouble(MAX_DPI);
            this.width = jsonObject.getInt(WIDTH);
            this.height = jsonObject.getInt(HEIGHT);
        }
    }
}
