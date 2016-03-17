package org.mapfish.print.map.geotools;

import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mapfish.print.Constants.Style.Raster.NAME;

/**
 * Abstract class for {@link org.mapfish.print.map.MapLayerFactoryPlugin} that created layers based on grid coverages.
 *
 * @author Jesse on 6/25/2014.
 */
public abstract class AbstractGridCoverageLayerPlugin {
    @Autowired
    private StyleParser styleParser;

    /**
     * Common method for creating styles.
     *
     * @param template the template that the map is part of
     * @param styleRef the style ref identifying the style
     * @param <T>      the source type
     */
    protected final <T> StyleSupplier<T> createStyleSupplier(final Template template,
                                                             final String styleRef) {
        return new StyleSupplier<T>() {
            @Override
            public Style load(final MfClientHttpRequestFactory requestFactory,
                              final T featureSource,
                              final MapfishMapContext mapContext) {
                final StyleParser parser = AbstractGridCoverageLayerPlugin.this.styleParser;
                return template.getStyle(styleRef, mapContext)
                        .or(parser.loadStyle(template.getConfiguration(), requestFactory, styleRef, mapContext))
                        .or(template.getConfiguration().getDefaultStyle(NAME));
            }
        };
    }

}
