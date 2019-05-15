package org.mapfish.print.map.style;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.xml.styling.SLDTransformer;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Parse a style using all the available {@link StyleParserPlugin} registered with the spring application
 * context.
 */
public final class StyleParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleParser.class);
    @Autowired
    private List<StyleParserPlugin> plugins = new ArrayList<>();

    /**
     * Load style using one of the plugins or return Optional.empty().
     *
     * @param configuration the configuration for the current request.
     * @param clientHttpRequestFactory a factory for making http requests
     * @param styleString the style to load.
     */
    public Optional<? extends Style> loadStyle(
            final Configuration configuration,
            @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
            final String styleString) {
        if (styleString != null) {
            for (StyleParserPlugin plugin: this.plugins) {
                final Optional<? extends Style> style = plugin.parseStyle(
                        configuration, clientHttpRequestFactory, styleString);
                if (style.isPresent()) {
                    if (LOGGER.isDebugEnabled()) {
                        try {
                            final SLDTransformer transformer = new SLDTransformer();
                            final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
                            final UserLayer userLayer = styleFactory.createUserLayer();
                            userLayer.addUserStyle(style.get());
                            final StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();
                            sld.addStyledLayer(userLayer);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Loaded style from: \n\n '{}': \n\n{}",
                                             styleString, transformer.transform(sld));
                            }
                        } catch (Exception e) {
                            LOGGER.debug(
                                    "Loaded style from: \n\n '{}' \n\n<Unable to transform it to xml>",
                                    styleString, e);
                        }
                    }
                    return style;
                }
            }
        }
        return Optional.empty();
    }
}
