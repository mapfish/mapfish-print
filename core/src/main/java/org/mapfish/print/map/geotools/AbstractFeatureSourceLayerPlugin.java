package org.mapfish.print.map.geotools;

import org.geotools.data.FeatureSource;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.Geometry;
import org.mapfish.print.OptionalUtils;
import org.mapfish.print.SetsUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Abstract class for FeatureSource based {@link org.mapfish.print.map.MapLayerFactoryPlugin} objects.
 *
 * @param <P> the type of parameter
 */
public abstract class AbstractFeatureSourceLayerPlugin<P> implements MapLayerFactoryPlugin<P> {

    private final Set<String> typeNames;
    /**
     * A parser for parsing styles.
     */
    @Autowired
    protected StyleParser parser;
    /**
     * A fork join pool for running async tasks.
     */
    @Autowired
    protected ExecutorService forkJoinPool;

    /**
     * Constructor.
     *
     * @param typeName at least one type name for identifying the plugin is required.
     * @param typeNames additional strings used to identify if this plugin can handle the layer
     *         definition.
     */
    public AbstractFeatureSourceLayerPlugin(final String typeName, final String... typeNames) {
        this.typeNames = SetsUtils.create(typeNames);
        this.typeNames.add(typeName);
    }

    @Override
    public final Set<String> getTypeNames() {
        return this.typeNames;
    }

    /**
     * Create a function that will create the style on demand.  This is called later in a separate thread so
     * any blocking calls will not block the parsing of the layer attributes.
     *
     * @param template the template for this map
     * @param styleString a string that identifies a style.
     */
    protected final StyleSupplier<FeatureSource> createStyleFunction(
            final Template template,
            final String styleString) {
        return new StyleSupplier<FeatureSource>() {
            @Override
            public Style load(
                    final MfClientHttpRequestFactory requestFactory,
                    final FeatureSource featureSource) {
                if (featureSource == null) {
                    throw new IllegalArgumentException("Feature source cannot be null");
                }

                final String geomType = featureSource.getSchema() == null ?
                        Geometry.class.getSimpleName().toLowerCase() :
                        featureSource.getSchema().getGeometryDescriptor().getType().getBinding()
                            .getSimpleName();
                final String styleRef = styleString != null ? styleString : geomType;

                final StyleParser styleParser = AbstractFeatureSourceLayerPlugin.this.parser;
                return OptionalUtils.or(
                        () -> template.getStyle(styleRef),
                        () -> styleParser.loadStyle(template.getConfiguration(), requestFactory, styleRef))
                        .orElseGet(() -> template.getConfiguration().getDefaultStyle(geomType));
            }
        };
    }

    public final void setParser(final StyleParser parser) {
        this.parser = parser;
    }
}
