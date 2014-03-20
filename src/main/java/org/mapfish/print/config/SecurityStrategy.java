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

package org.mapfish.print.config;

import org.apache.commons.httpclient.HttpClient;

import java.io.IOException;
import java.net.URI;

/**
 * A strategy for authenticating with a URI.
 */
public abstract class SecurityStrategy implements ConfigurationObject {
    private HostMatcher matcher;

    /**
     * Configure security of the http client for the uri.
     *
     * @param uri uri of request
     * @param httpClient http client which will make request.
     */
    public abstract void configure(URI uri, HttpClient httpClient);

    /**
     * Return true if this strategy can be used for the provided URI.
     *
     * @param uri the uri to match against.
     * @return true if this strategy can be used for the provided URI.
     */
    public final boolean matches(final URI uri) {
        try {
            return this.matcher == null || this.matcher.validate(uri);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Set the matching strategy for determining if this strategy can be used to secure a give URL.
     * @param matcher the matcher.
     */
    public final void setMatcher(final HostMatcher matcher) {
        this.matcher = matcher;
    }
}
