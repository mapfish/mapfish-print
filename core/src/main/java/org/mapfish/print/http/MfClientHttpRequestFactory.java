/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.http;

import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * A http request factory that allows configuration callbacks to be registered, allowing low-level customizations to the request
 * object.
 *
 * @author Jesse on 9/3/2014.
 */
public interface MfClientHttpRequestFactory extends ClientHttpRequestFactory {

    /**
     * Register a callback for config using a http request.
     *
     * @param callback the configuration callback
     */
    void register(final RequestConfigurator callback);
    /**
     * A Callback allowing low-level customizations to an http request created by this factory.
     */
    public interface RequestConfigurator {
        /**
         * Configure the request.
         *
         * @param request the request to configure
         */
        void configureRequest(final ConfigurableRequest request);
    }
}
