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

package org.mapfish.print.map.geotools;

import com.google.common.base.Optional;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * Parses GeoJson from the requestData.
 *
 * @author Jesse on 3/26/14.
 */
public final class GeoJsonLayer extends AbstractFeatureSourceLayer {

    /**
     * Constructor.
     *
     * @param featureSource the featureSource containing the feature data.
     * @param style         style to use for rendering the data.
     */
    public GeoJsonLayer(final FeatureSource featureSource, final Style style) {
        super(featureSource, style);
    }

    /**
     * Parser for creating {@link org.mapfish.print.map.geotools.GeoJsonLayer} layers from request data.
     */
    public static final class Plugin implements MapLayerFactoryPlugin {

        private static final String TYPE = "geojson";
        private static final String COMPATIBILITY_TYPE = "vector";
        private static final String JSON_DATA = "geoJson";

        private final FeatureJSON geoJsonReader = new FeatureJSON();

        @Autowired
        private StyleParser parser;

        @Nonnull
        @Override
        public Optional<? extends MapLayer> parse(final Template template, @Nonnull final PJsonObject layerJson) throws IOException {
            final Optional<? extends MapLayer> result;
            final String type = layerJson.getString("type");
            final String geoJsonString = layerJson.optString(JSON_DATA);
            if (TYPE.equalsIgnoreCase(type) ||
                COMPATIBILITY_TYPE.equalsIgnoreCase(type) && geoJsonString != null) {

                final byte[] bytes = geoJsonString.getBytes(Constants.ENCODING);
                final ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                SimpleFeatureCollection featureCollection = (SimpleFeatureCollection) this.geoJsonReader.readFeatureCollection(input);
                FeatureSource featureSource = new CollectionFeatureSource(featureCollection);

                final String styleRef = layerJson.getString("style");

                Style style = template.getStyle(styleRef)
                        .or(this.parser.loadStyle(template.getConfiguration(), styleRef))
                        .or(template.getDefaultStyle());

                result = Optional.of(new GeoJsonLayer(featureSource, style));
            } else {
                result = Optional.absent();
            }
            return result;
        }
    }
}
