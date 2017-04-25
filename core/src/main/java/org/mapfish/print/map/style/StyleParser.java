package org.mapfish.print.map.style;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Parse a style using all the available {@link StyleParserPlugin} registered with the spring application context.
 */
public final class StyleParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(StyleParser.class);
 @Autowired
 private List<StyleParserPlugin> plugins = Lists.newArrayList();

    /**
     * Load style using one of the plugins or return Optional.absent().
     *  @param configuration the configuration for the current request.
     * @param clientHttpRequestFactory a factory for making http requests
     * @param styleString   the style to load.
     */
    public Optional<? extends Style> loadStyle(final Configuration configuration,
                                               @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
                                               final String styleString) {
        if (styleString != null) {
        for (StyleParserPlugin plugin : this.plugins) {
            try {
                Optional<? extends Style> style = plugin.parseStyle(configuration, clientHttpRequestFactory, styleString);
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
                                LOGGER.debug("Loaded style from: \n\n '" + styleString + "': \n\n" + transformer.transform(sld));
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Loaded style from: \n\n '" + styleString + "' \n\n<Unable to transform it to xml>: " + e, e);
                        }
                    }
                    return style;
                }
            } catch (Throwable t) {
                throw ExceptionUtils.getRuntimeException(t);
            }
        }
        }
        return Optional.absent();
    }
}
