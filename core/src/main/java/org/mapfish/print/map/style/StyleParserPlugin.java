package org.mapfish.print.map.style;

import com.google.common.base.Optional;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A plugin used for loading {@link org.geotools.styling.Style} objects from a string.
 *
 * The string might be json, css, url, whatever.
 *
* @author Jesse on 3/26/14.
*/
public interface StyleParserPlugin {

    /**
     * Using the string load a style.  The string can be from a URL, xml, css, whatever.  If the string
     * references a file it <strong>MUST</strong> be within a subdirectory of the configuration directory.
     *
     * @param configuration the configuration being used for the current print.
     * @param clientHttpRequestFactory an factory for making http requests.
     * @param styleString the string that provides the information for loading the style.
     * @param mapContext information about the map projection, bounds, size, etc...
     *
     * @return if this plugin can create a style form the string then return the style otherwise Optional.absent().
     */
    Optional<Style> parseStyle(@Nullable Configuration configuration,
                               @Nonnull ClientHttpRequestFactory clientHttpRequestFactory,
                               @Nullable String styleString,
                               @Nonnull MapfishMapContext mapContext) throws Throwable;
}
