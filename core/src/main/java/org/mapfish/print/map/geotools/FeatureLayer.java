package org.mapfish.print.map.geotools;

import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.styling.Style;
import org.mapfish.print.OptionalUtils;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

/**
 * A layer to render GeoTools features.
 * <p>
 * This layer type is only intended for internal use, for example to render the bbox rectangle in the overview
 * map.
 */
public final class FeatureLayer extends AbstractFeatureSourceLayer {

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be
     *         called once.
     * @param styleSupplier a function that creates the style for styling the features. This will only
     *         be called once.
     * @param renderAsSvg is the layer rendered as SVG?
     * @param params the parameters for this layer
     */
    public FeatureLayer(
            final ExecutorService executorService,
            final FeatureSourceSupplier featureSourceSupplier,
            final StyleSupplier<FeatureSource> styleSupplier,
            final boolean renderAsSvg,
            final AbstractLayerParams params) {
        super(executorService, featureSourceSupplier, styleSupplier, renderAsSvg, params);
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
        public FeatureLayer parse(
                @Nonnull final Template template,
                @Nonnull final FeatureLayerParam param) {
            return new FeatureLayer(
                    this.forkJoinPool,
                    createFeatureSourceSupplier(param.features),
                    createStyleFunction(template, param.style, param.defaultStyle),
                    template.getConfiguration().renderAsSvg(param.renderAsSvg),
                    param);
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
         * Create a function that will create the style on demand.  This is called later in a separate thread
         * so any blocking calls will not block the parsing of the layer attributes.
         *
         * @param template the template for this map
         * @param styleString a string that identifies a style.
         * @param defaultStyleName a custom name for the default style. If null, the default style is
         *         selected depending on the geometry type.
         */
        protected StyleSupplier<FeatureSource> createStyleFunction(
                final Template template, final String styleString, final String defaultStyleName) {
            return new StyleSupplier<FeatureSource>() {
                @Override
                public Style load(
                        final MfClientHttpRequestFactory requestFactory,
                        final FeatureSource featureSource) {
                    if (featureSource == null) {
                        throw new IllegalArgumentException("Feature source cannot be null");
                    }

                    final String geomType =
                            featureSource.getSchema().getGeometryDescriptor().getType().getBinding()
                            .getSimpleName();
                    final String styleRef = styleString != null ? styleString : (defaultStyleName != null ?
                            defaultStyleName : geomType);
                    return OptionalUtils.or(
                            () -> template.getStyle(styleRef),
                            () -> Plugin.this.parser.loadStyle(template.getConfiguration(), requestFactory,
                                                               styleRef))
                            .orElseGet(() -> template.getConfiguration().getDefaultStyle(styleRef));
                }
            };
        }
    }

    /**
     * The parameters for creating a vector layer.
     */
    public static class FeatureLayerParam extends AbstractLayerParams {
        /**
         * A collection of features.
         */
        public SimpleFeatureCollection features;
        /**
         * The style name of a style to apply to the features during rendering.  The style name must map to a
         * style in the template or the configuration objects.
         *
         * If no style is defined then the default style for the geometry type will be used.
         */
        public String style;
        /**
         * If no style is defined, a default style with this name will be used. Otherwise a style will be
         * selected depending on the the geometry type.
         */
        public String defaultStyle;
        /**
         * Indicates if the layer is rendered as SVG.
         *
         * (will default to {@link org.mapfish.print.config.Configuration#defaultToSvg}).
         */
        public Boolean renderAsSvg;
    }
}
