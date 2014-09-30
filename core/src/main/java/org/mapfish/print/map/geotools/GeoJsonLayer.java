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

import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;

import java.io.IOException;
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
     * @param renderAsSvg           is the layer rendered as SVG?
     */
    public GeoJsonLayer(final ExecutorService executorService,
                        final FeatureSourceSupplier featureSourceSupplier,
                        final StyleSupplier<FeatureSource> styleSupplier,
                        final boolean renderAsSvg) {
        super(executorService, featureSourceSupplier, styleSupplier, renderAsSvg);
    }

    /**
     * Parser for creating {@link org.mapfish.print.map.geotools.GeoJsonLayer} layers from request data.
     */
    public static final class Plugin extends AbstractFeatureSourceLayerPlugin<GeoJsonParam> {

        private static final String TYPE = "geojson";
        private static final String COMPATIBILITY_TYPE = "vector";

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
        public GeoJsonLayer parse(@Nonnull final Template template,
                                  @Nonnull final GeoJsonParam param) throws IOException {
            return new GeoJsonLayer(
                    this.forkJoinPool,
                    createFeatureSourceSupplier(template, param.geoJson),
                    createStyleFunction(template, param.style),
                    template.getConfiguration().renderAsSvg(param.renderAsSvg));
        }

        private FeatureSourceSupplier createFeatureSourceSupplier(final Template template,
                                                                    final String geoJsonString) {
            return new FeatureSourceSupplier() {
                @Nonnull
                @Override
                public FeatureSource load(@Nonnull final MfClientHttpRequestFactory requestFactory,
                                          @Nonnull final MapfishMapContext mapContext) {
                    final FeaturesParser parser = new FeaturesParser(requestFactory, mapContext.isForceLongitudeFirst());
                    SimpleFeatureCollection featureCollection;
                    try {
                        featureCollection = parser.autoTreat(template, geoJsonString);
                        return new CollectionFeatureSource(featureCollection);
                    } catch (IOException e) {
                        throw ExceptionUtils.getRuntimeException(e);
                    }
                }
            };
        }
    }

    /**
     * The parameters for creating a layer that renders GeoJSON formatted data.
     */
    public static class GeoJsonParam extends AbstractVectorLayerParam {
        /**
         * A geojson formatted string or url to the geoJson or the raw GeoJSON data.
         * <p/>
         * The url can be a file url, however if it is it must be relative to the configuration directory.
         */
        public String geoJson;
    }
}
