/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.config;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to validate the access to a map service host
 */
public abstract class HostMatcher {
    public final static HostMatcher ACCEPT_ALL = new HostMatcher() {
        @Override
        public boolean validate(URI uri) throws UnknownHostException, SocketException, MalformedURLException {
            return true;
        }

        @Override
        public String toString() {
            return "Accept All";
        }

    };

    protected int port = -1;
    protected String pathRegex = null;

    public boolean validate(URI uri) throws UnknownHostException, SocketException, MalformedURLException {
        int uriPort = uri.getPort();
        if (uriPort < 0) {
            uriPort = uri.toURL().getDefaultPort();
        }
        if (port > 0 && uriPort != port) {
            return false;
        }

        if (pathRegex != null) {
            Matcher matcher = Pattern.compile(pathRegex).matcher(uri.getPath());
            if (!matcher.matches()) {
                return false;
            }
        }
        return true;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPathRegex(String pathRegex) {
        this.pathRegex = pathRegex;
    }

    public abstract String toString();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((pathRegex == null) ? 0 : pathRegex.hashCode());
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HostMatcher other = (HostMatcher) obj;
        if (pathRegex == null) {
            if (other.pathRegex != null)
                return false;
        } else if (!pathRegex.equals(other.pathRegex))
            return false;
        if (port != other.port)
            return false;
        return true;
    }
}
