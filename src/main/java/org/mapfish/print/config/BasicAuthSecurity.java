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

public class BasicAuthSecurity extends SecurityStrategy {

    String username = null;
    String password = null;
    boolean preemptive = false;

    @Override
    public void configure(URI uri, HttpClient httpClient) {
        if (username == null || password == null) {
            throw new IllegalStateException("username and password configuration of BasicAuthSecurity is required");
        }

        if (preemptive) {
            httpClient.getParams().setAuthenticationPreemptive(true);
        }
        httpClient.getState().setCredentials(new AuthScope(uri.getHost(), uri.getPort()),
                new UsernamePasswordCredentials(username, password));
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPreemptive(boolean preemptive) {
        this.preemptive = preemptive;
    }
}
