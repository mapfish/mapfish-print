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
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import java.net.URI;
/**
 * Authenticate using basic auth.
 * 
 * @author Jesse
 */
public class BasicAuthSecurity extends SecurityStrategy {

    private String username = null;
    private String password = null;
    private boolean preemptive = false;

    @Override
	public final void configure(final URI uri, final HttpClient httpClient) {
        if (this.username == null || this.password == null) {
            throw new IllegalStateException("username and password configuration of BasicAuthSecurity is required");
        }

        if (this.preemptive) {
            httpClient.getParams().setAuthenticationPreemptive(true);
        }
        httpClient.getState().setCredentials(new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(this.username, this.password));
    }

    public final void setUsername(final String username) {
        this.username = username;
    }

    public final void setPassword(final String password) {
        this.password = password;
    }

    public final void setPreemptive(final boolean preemptive) {
        this.preemptive = preemptive;
    }
}
