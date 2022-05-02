package org.mapfish.print.map.geotools;

import org.geotools.styling.Style;
import org.mapfish.print.http.MfClientHttpRequestFactory;

/**
 * A strategy for loading style objects.
 *
 * @param <SOURCE> the type source that the style applies to
 */
public interface StyleSupplier<SOURCE> {
    /**
     * Load the style.
     *
     * @param requestFactory the factory to use for making http requests
     * @param featureSource the source the style applies to
     */
    Style load(MfClientHttpRequestFactory requestFactory, SOURCE featureSource);
}
