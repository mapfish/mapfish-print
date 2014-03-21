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
import org.mapfish.print.json.PJsonArray;
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
    public final MapAttributeValues getValue(final PJsonObject values, final String name) {
        return new MapAttributeValues(values.getJSONObject(name));
    }

    @Override
    protected final String getType() {
        return "map";
    }

    @Override
    protected final void additionalPrintClientConfig(final JSONWriter json) throws JSONException {
        json.key(MAX_DPI).value(this.maxDpi);
        json.key(WIDTH).value(this.width);
        json.key(HEIGHT).value(this.height);
    }

    public final void setMaxDpi(final float maxDpi) {
        this.maxDpi = maxDpi;
    }

    public final void setWidth(final float width) {
        this.width = width;
    }

    public final void setHeight(final float height) {
        this.height = height;
    }

    /**
     * The value of {@link org.mapfish.print.attribute.MapAttribute}.
     */
    public static class MapAttributeValues {
        static final String CENTER = "center";
        static final String SCALE = "scale";
        static final String ROTATION = "rotation";
        static final String LAYERS = "layers";

        private final PJsonArray center;
        private final double scale;
        private final double rotation;
        private final PJsonArray layers;

        /**
         * Constructor.
         * @param jsonObject json containing attribute information.
         */
        public MapAttributeValues(final PJsonObject jsonObject) {
            this.center = jsonObject.getJSONArray(CENTER);
            this.scale = jsonObject.getDouble(SCALE);
            this.rotation = jsonObject.getInt(ROTATION);
            this.layers = jsonObject.getJSONArray(LAYERS);
        }

        public final PJsonArray getCenter() {
            return this.center;
        }

        public final double getScale() {
            return this.scale;
        }

        public final double getRotation() {
            return this.rotation;
        }

        public final PJsonArray getLayers() {
            return this.layers;
        }
    }
}
