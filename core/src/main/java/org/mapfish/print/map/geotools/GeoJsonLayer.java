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

import com.google.common.collect.Sets;
import jsr166y.ForkJoinPool;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.mapfish.print.processor.HasDefaultValue;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
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
    public static final class Plugin implements MapLayerFactoryPlugin<GeoJsonParam> {

        private static final String TYPE = "geojson";
        private static final String COMPATIBILITY_TYPE = "vector";

        private final FeatureJSON geoJsonReader = new FeatureJSON();

        @Autowired
        private StyleParser parser;
        @Autowired
        private ForkJoinPool forkJoinPool;
        private Set<String> typeNames = Sets.newHashSet(TYPE, COMPATIBILITY_TYPE);


        @Override
        public Set<String> getTypeNames() {
            return this.typeNames;
        }

        @Override
        public GeoJsonParam createParameter() {
            return new GeoJsonParam();
        }

        @Nonnull
        @Override
        public GeoJsonLayer parse(final Template template, @Nonnull final GeoJsonParam param) throws IOException {
            SimpleFeatureCollection featureCollection = treatStringAsURL(template, param.geoJson);
            if (featureCollection == null) {
                featureCollection = treatStringAsGeoJson(param.geoJson);
            }
            FeatureSource featureSource = new CollectionFeatureSource(featureCollection);

            String geomType = featureCollection.getSchema().getGeometryDescriptor().getType().getBinding().getSimpleName();
            String styleRef = param.style;

            if (styleRef == null) {
                styleRef = geomType;
            }
            Style style = template.getStyle(styleRef)
                    .or(this.parser.loadStyle(template.getConfiguration(), styleRef))
                    .or(template.getConfiguration().getDefaultStyle(geomType));

            return new GeoJsonLayer(featureSource, style, this.forkJoinPool);
        }

        private SimpleFeatureCollection treatStringAsURL(final Template template, final String geoJsonString) throws IOException {
            try {
                URL url = new URL(geoJsonString);
                final String protocol = url.getProtocol();
                if (protocol.equalsIgnoreCase("file")) {

                    File file = new File(template.getConfiguration().getDirectory(), geoJsonString.substring("file://".length()));
                    if (file.exists() && file.isFile()) {
                        url = file.getAbsoluteFile().toURI().toURL();
                        assertFileIsInConfigDir(template, file);
                    } else {
                        throw new IllegalArgumentException(url + " is not a relative URL file.  File urls are always interpreted as " +
                                                           "being relative to the configuration directory.");
                    }
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

        private void assertFileIsInConfigDir(final Template template, final File file) {
            final String configurationDir = template.getConfiguration().getDirectory().getAbsolutePath();
            if (!file.getAbsolutePath().startsWith(configurationDir)) {
                throw new IllegalArgumentException("The geoJson attribute is a file url but indicates a file that is not within the" +
                                                   " configurationDirectory: " + file.getAbsolutePath());
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

    /**
     * The parameters for creating a layer that renders GeoJSON formatted data.
     */
    public static class GeoJsonParam {
        /**
         * A url to the geoJson or the raw GeoJSON data.
         * <p/>
         * The url can be a file url, however if it is it must be relative to the configuration directory.
         */
        public String geoJson;
        /**
         * The style name of a style to apply to the features during rendering.  The style name must map to a style in the
         * template or the configuration objects.
         * <p/>
         * If no style is defined then the default style for the geometry type will be used.
         */
        @HasDefaultValue
        public String style;
    }
}
