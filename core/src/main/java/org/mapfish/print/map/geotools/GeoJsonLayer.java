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

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.io.CharSource;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.FileUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.parser.HasDefaultValue;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
     * @param executorService       the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be called once.
     * @param styleSupplier         a function that creates the style for styling the features. This will only be called once.
     */
    public GeoJsonLayer(final ExecutorService executorService, final Supplier<FeatureSource> featureSourceSupplier,
                        final Function<FeatureSource, Style> styleSupplier) {
        super(executorService, featureSourceSupplier, styleSupplier);
    }

    /**
     * Parser for creating {@link org.mapfish.print.map.geotools.GeoJsonLayer} layers from request data.
     */
    public static final class Plugin extends AbstractFeatureSourceLayerPlugin<GeoJsonParam> {

        private static final String TYPE = "geojson";
        private static final String COMPATIBILITY_TYPE = "vector";

        private final FeatureJSON geoJsonReader = new FeatureJSON();

        /**
         * Constructor.
         */
        public Plugin() {
            super(TYPE, COMPATIBILITY_TYPE);
        }

        @Override
        public GeoJsonParam createParameter() {
            return new GeoJsonParam();
        }

        @Nonnull
        @Override
        public GeoJsonLayer parse(@Nonnull final Template template, @Nonnull final GeoJsonParam param) throws IOException {
            return new GeoJsonLayer(
                    this.forkJoinPool,
                    createFeatureSourceSupplier(template, param.geoJson),
                    createStyleFunction(template, param.style));
        }

        private Supplier<FeatureSource> createFeatureSourceSupplier(final Template template, final String geoJsonString) {
            return new Supplier<FeatureSource>() {
                @Override
                public FeatureSource get() {
                    SimpleFeatureCollection featureCollection;
                    try {
                        featureCollection = treatStringAsURL(template, geoJsonString);
                        if (featureCollection == null) {
                            featureCollection = treatStringAsGeoJson(geoJsonString);
                        }
                        return new CollectionFeatureSource(featureCollection);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        private SimpleFeatureCollection treatStringAsURL(final Template template, final String geoJsonString) throws IOException {
            URL url;
            try {
                url = FileUtils.testForLegalFileUrl(template.getConfiguration(), new URL(geoJsonString));
            } catch (MalformedURLException e) {
                return null;
            }

            Closer closer = Closer.create();
            try {
                Reader input;
                if (url.getProtocol().equalsIgnoreCase("file")) {
                    final CharSource charSource = Files.asCharSource(new File(url.getFile()), Constants.DEFAULT_CHARSET);
                    input = closer.register(charSource.openBufferedStream());
                } else {
                    final ClientHttpResponse response = closer.register(this.httpRequestFactory.createRequest(url.toURI(),
                            HttpMethod.GET).execute());

                    input = closer.register(new BufferedReader(new InputStreamReader(response.getBody(), Constants.DEFAULT_CHARSET)));
                }
                return (SimpleFeatureCollection) this.geoJsonReader.readFeatureCollection(input);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            } finally {
                closer.close();
            }
        }

        private SimpleFeatureCollection treatStringAsGeoJson(final String geoJsonString) throws IOException {
            final byte[] bytes = geoJsonString.getBytes(Constants.DEFAULT_ENCODING);
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
