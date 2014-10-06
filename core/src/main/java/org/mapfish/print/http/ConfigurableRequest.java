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

import org.apache.http.client.methods.HttpRequestBase;
import org.mapfish.print.config.Configuration;
import org.springframework.http.client.ClientHttpRequest;

/**
 * A request object that provides low-level access so that the request can be configured for proxying, authentication, etc...
 */
public interface ConfigurableRequest extends ClientHttpRequest {
    /**
     * Obtain the request object.
     */
    HttpRequestBase getUnderlyingRequest();

    /**
     * Set the current configuration object.  This should only be called by
     * {@link org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory}.
     *
     * @param configuration the config object for the current print job.
     */
    void setConfiguration(final Configuration configuration);
}
