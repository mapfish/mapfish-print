package org.mapfish.print.http;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * A http request factory that allows configuration callbacks to be registered, allowing low-level
 * customizations to the request object.
 */
public interface MfClientHttpRequestFactory extends ClientHttpRequestFactory {

    /**
     * Register a callback for config using a http request.
     *
     * @param callback the configuration callback
     */
    void register(RequestConfigurator callback);

    /**
     * A Callback allowing low-level customizations to an http request created by this factory.
     */
    interface RequestConfigurator {
        /**
         * Configure the request.
         *
         * @param request the request to configure
         */
        void configureRequest(ClientHttpRequest request);
    }
}
