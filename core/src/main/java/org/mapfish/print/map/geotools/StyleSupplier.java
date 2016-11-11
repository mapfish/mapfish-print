package org.mapfish.print.map.geotools;

import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;

/**
 * A strategy for loading style objects.
 *
 * @param <Source> the type source that the style applies to
 */
public interface StyleSupplier<Source> {
    /**
     * Load the style.
     * @param requestFactory the factory to use for making http requests
     * @param featureSource the source the style applies to
     * @param mapContext information about the map projection, bounds, size, etc...
     */
    Style load(final MfClientHttpRequestFactory requestFactory,
               final Source featureSource,
               final MapfishMapContext mapContext) throws Exception;
}
