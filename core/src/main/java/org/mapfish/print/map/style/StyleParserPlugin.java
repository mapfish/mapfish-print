package org.mapfish.print.map.style;

import org.geotools.styling.Style;
import org.mapfish.print.config.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A plugin used for loading {@link Style} objects from a string.
 * <p>
 * The string might be json, css, url, whatever.
 */
public interface StyleParserPlugin {

    /**
     * Using the string load a style.  The string can be from a URL, xml, css, whatever.  If the string
     * references a file it <strong>MUST</strong> be within a subdirectory of the configuration directory.
     *
     * @param configuration the configuration being used for the current print.
     * @param clientHttpRequestFactory an factory for making http requests.
     * @param styleString the string that provides the information for loading the style.
     * @return if this plugin can create a style form the string then return the style otherwise
     *         Optional.absent().
     */
    Optional<Style> parseStyle(
            @Nullable Configuration configuration,
            @Nonnull ClientHttpRequestFactory clientHttpRequestFactory,
            @Nonnull String styleString);
}
