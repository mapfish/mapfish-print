package org.mapfish.print.processor.http;

import org.mapfish.print.http.MfClientHttpRequestFactory;

/**
 * Used to be compatible with the InputOutputValues and be able change the MfClientHttpRequestFactory.
 */
public class MfClientHttpRequestFactoryProvider {
    private MfClientHttpRequestFactory clientHttpRequestFactory;

    /**
     * Construct.
     *
     * @param initialClientHttpRequestFactory The initial value
     */
    public MfClientHttpRequestFactoryProvider(
            final MfClientHttpRequestFactory initialClientHttpRequestFactory) {
        this.clientHttpRequestFactory = initialClientHttpRequestFactory;
    }

    /**
     * Get the current value.
     *
     * @return the current value
     */
    public MfClientHttpRequestFactory get() {
        return this.clientHttpRequestFactory;
    }

    /**
     * Set the value.
     *
     * @param newClientHttpRequestFactory the new value
     */
    public void set(final MfClientHttpRequestFactory newClientHttpRequestFactory) {
        this.clientHttpRequestFactory = newClientHttpRequestFactory;
    }
}
