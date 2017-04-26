package org.mapfish.print.map.geotools;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;

import org.geotools.data.FeatureSource;
import org.geotools.styling.Style;
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
 *
 * CSOFF:VisibilityModifier
 */
public abstract class AbstractFeatureSourceLayerPlugin<P> implements MapLayerFactoryPlugin<P> {

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

    private final Set<String> typeNames;

    /**
     * Constructor.
     *
     * @param typeName at least one type name for identifying the plugin is required.
     * @param typeNames additional strings used to identify if this plugin can handle the layer definition.
     */
    public AbstractFeatureSourceLayerPlugin(final String typeName, final String... typeNames) {
        this.typeNames = Sets.newHashSet(typeNames);
        this.typeNames.add(typeName);
    }

    @Override
    public final Set<String> getTypeNames() {
        return this.typeNames;
    }

    /**
     * Create a function that will create the style on demand.  This is called later in a separate thread so any blocking calls
     * will not block the parsing of the layer attributes.
     * @param template the template for this map
     * @param styleString a string that identifies a style.
     */
    protected final StyleSupplier<FeatureSource> createStyleFunction(final Template template,
                                                                       final String styleString) {
        return new StyleSupplier<FeatureSource>() {
            @Override
            public Style load(final MfClientHttpRequestFactory requestFactory,
                              final FeatureSource featureSource) {
                if (featureSource == null) {
                    throw new IllegalArgumentException("Feature source cannot be null");
                }

                String geomType = Geometry.class.getSimpleName().toLowerCase();
                if (featureSource.getSchema() != null) {
                    geomType = featureSource.getSchema().getGeometryDescriptor().getType().getBinding().getSimpleName();
                }
                String styleRef = styleString;

                if (styleRef == null) {
                    styleRef = geomType;
                }

                final StyleParser styleParser = AbstractFeatureSourceLayerPlugin.this.parser;
                return template.getStyle(styleRef)
                        .or(styleParser.loadStyle(template.getConfiguration(), requestFactory, styleRef))
                        .or(template.getConfiguration().getDefaultStyle(geomType));
            }
        };
    }

    public final void setParser(final StyleParser parser) {
        this.parser = parser;
    }
}
