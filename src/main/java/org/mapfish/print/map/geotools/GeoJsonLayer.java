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
import jsr166y.ForkJoinPool;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
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
     * @param featureSource   the featureSource containing the feature data.
     * @param style           style to use for rendering the data.
     * @param executorService the thread pool for doing the rendering.
     */
    public GeoJsonLayer(final FeatureSource featureSource, final Style style, final ExecutorService executorService) {
        super(featureSource, style, executorService);
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
        @Autowired
        private ForkJoinPool forkJoinPool;


        @Nonnull
        @Override
        public Optional<GeoJsonLayer> parse(final Template template, @Nonnull final PJsonObject layerJson) throws IOException {
            final Optional<GeoJsonLayer> result;
            final String type = layerJson.getString("type");
            final String geoJsonString = layerJson.optString(JSON_DATA);
            if (TYPE.equalsIgnoreCase(type) ||
                COMPATIBILITY_TYPE.equalsIgnoreCase(type) && geoJsonString != null) {

                SimpleFeatureCollection featureCollection = treatStringAsURL(template, geoJsonString);
                if (featureCollection == null) {
                    featureCollection = treatStringAsGeoJson(geoJsonString);
                }
                FeatureSource featureSource = new CollectionFeatureSource(featureCollection);

                final String styleRef = layerJson.getString("style");

                String geomType = featureCollection.getSchema().getGeometryDescriptor().getType().getBinding().getSimpleName();
                Style style = template.getStyle(styleRef)
                        .or(this.parser.loadStyle(template.getConfiguration(), styleRef))
                        .or(template.getConfiguration().getDefaultStyle(geomType));

                result = Optional.of(new GeoJsonLayer(featureSource, style, this.forkJoinPool));
            } else {
                result = Optional.absent();
            }
            return result;
        }

        private SimpleFeatureCollection treatStringAsURL(final Template template, final String geoJsonString) throws IOException {
            try {
                URL url = new URL(geoJsonString);
                final String protocol = url.getProtocol();
                if (protocol.equalsIgnoreCase("file")) {
                    final File file = new File(template.getConfiguration().getDirectory(), geoJsonString.substring("file://".length()));
                    if (file.exists() && file.isFile()) {
                        url = file.getAbsoluteFile().toURI().toURL();
                    }
                    assertFileIsInConfigDir(template, url);
                }
                InputStream input = null;
                try {
                    input = url.openStream();
                    return (SimpleFeatureCollection) this.geoJsonReader.readFeatureCollection(input);
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
            } catch (MalformedURLException e) {
                return null;
            }
        }

        private void assertFileIsInConfigDir(final Template template, final URL url) {
            try {
                final File file = new File(url.toURI());
                final String configurationDir = template.getConfiguration().getDirectory().getAbsolutePath();
                if (!file.getAbsolutePath().startsWith(configurationDir)) {
                    throw new IllegalArgumentException("The geoJson attribute is a file url but indicates a file that is not within the" +
                                                       " configurationDirectory: " + file.getAbsolutePath());
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        private SimpleFeatureCollection treatStringAsGeoJson(final String geoJsonString) throws IOException {
            final byte[] bytes = geoJsonString.getBytes(Constants.ENCODING);
            ByteArrayInputStream input = null;
            try {
                input = new ByteArrayInputStream(bytes);
                return (SimpleFeatureCollection) this.geoJsonReader.readFeatureCollection(input);
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        }
    }
}
