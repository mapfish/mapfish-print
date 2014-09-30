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
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

/**
 * A layer to render GeoTools features.
 *
 * This layer type is only intended for internal use, for example
 * to render the bbox rectangle in the overview map.
 */
public final class FeatureLayer extends AbstractFeatureSourceLayer {

    /**
     * Constructor.
     *
     * @param executorService       the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be called once.
     * @param styleSupplier         a function that creates the style for styling the features. This will only be called once.
     * @param renderAsSvg           is the layer rendered as SVG?
     */
    public FeatureLayer(final ExecutorService executorService,
                        final FeatureSourceSupplier featureSourceSupplier,
                        final StyleSupplier<FeatureSource> styleSupplier,
                        final boolean renderAsSvg) {
        super(executorService, featureSourceSupplier, styleSupplier, renderAsSvg);
    }

    /**
     * Parser for creating {@link org.mapfish.print.map.geotools.FeatureLayer} layers from request data.
     */
    public static final class Plugin extends AbstractFeatureSourceLayerPlugin<FeatureLayerParam> {

        private static final String TYPE = "feature";

        /**
         * Constructor.
         */
        public Plugin() {
            super(TYPE);
        }

        @Override
        public FeatureLayerParam createParameter() {
            return new FeatureLayerParam();
        }

        @Nonnull
        @Override
        public FeatureLayer parse(@Nonnull final Template template,
                                  @Nonnull final FeatureLayerParam param) throws IOException {
            return new FeatureLayer(
                    this.forkJoinPool,
                    createFeatureSourceSupplier(param.features),
                    createStyleFunction(template, param.style, param.defaultStyle),
                    template.getConfiguration().renderAsSvg(param.renderAsSvg));
        }

        private FeatureSourceSupplier createFeatureSourceSupplier(
                final SimpleFeatureCollection features) {
            return new FeatureSourceSupplier() {
                @Override
                public FeatureSource load(
                        final MfClientHttpRequestFactory requestFactory,
                        final MapfishMapContext mapContext) {
                    return new CollectionFeatureSource(features);
                }
            };
        }

        /**
         * Create a function that will create the style on demand.  This is called later in a separate thread so any blocking calls
         * will not block the parsing of the layer attributes.
         * @param template          the template for this map
         * @param styleString       a string that identifies a style.
         * @param defaultStyleName  a custom name for the default style. If null, the default style is selected
         *      depending on the geometry type.
         */
        protected StyleSupplier<FeatureSource> createStyleFunction(final Template template,
                                                                   final String styleString, final String defaultStyleName) {
            return new StyleSupplier<FeatureSource>() {
                @Override
                public Style load(final MfClientHttpRequestFactory requestFactory,
                                  final FeatureSource featureSource,
                                  final MapfishMapContext mapContext) {
                    if (featureSource == null) {
                        throw new IllegalArgumentException("Feature source cannot be null");
                    }

                    String geomType = featureSource.getSchema().getGeometryDescriptor().getType().getBinding().getSimpleName();
                    String styleRef = styleString;

                    if (styleRef == null) {
                        if (defaultStyleName != null) {
                            styleRef = defaultStyleName;
                        } else {
                            styleRef = geomType;
                        }
                    }
                    return template.getStyle(styleRef, mapContext)
                            .or(Plugin.this.parser.loadStyle(
                                    template.getConfiguration(),
                                    requestFactory, styleRef, mapContext))
                            .or(template.getConfiguration().getDefaultStyle(styleRef));
                }
            };
        }
    }

    /**
     * The parameters for creating a vector layer.
     */
    public static class FeatureLayerParam {
        /**
         * A collection of features.
         */
        public SimpleFeatureCollection features;
        /**
         * The style name of a style to apply to the features during rendering.  The style name must map to a style in the
         * template or the configuration objects.
         * <p/>
         * If no style is defined then the default style for the geometry type will be used.
         */
        public String style;
        /**
         * If no style is defined, a default style with this name will be used. Otherwise
         * a style will be selected depending on the the geometry type.
         */
        public String defaultStyle;
        /**
         * Indicates if the layer is rendered as SVG.
         * <p/>
         * (will default to {@link org.mapfish.print.config.Configuration#defaultStyle}).
         */
        public Boolean renderAsSvg;
    }
}
