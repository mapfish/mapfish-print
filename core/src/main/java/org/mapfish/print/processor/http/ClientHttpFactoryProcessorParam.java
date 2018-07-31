package org.mapfish.print.processor.http;

import org.mapfish.print.processor.InputOutputValue;

/**
 * The parameter for a processors that have {@link org.mapfish.print.http.MfClientHttpRequestFactory}.
 */
public class ClientHttpFactoryProcessorParam {
    /**
     * The object for creating requests.  There should always be an instance in the values object so it does
     * not need to be created.
     */
    @InputOutputValue
    public MfClientHttpRequestFactoryProvider clientHttpRequestFactoryProvider;
}
