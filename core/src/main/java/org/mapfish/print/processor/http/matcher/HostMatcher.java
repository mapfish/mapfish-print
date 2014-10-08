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

package org.mapfish.print.processor.http.matcher;

import com.google.common.base.Optional;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to validate the access to a map service host.
 */
public abstract class HostMatcher implements URIMatcher {
    /**
     * The request port.  -1 is the unset/default number
     * CSOFF: VisibilityModifier
     */
    protected int port = -1;
    /**
     * A regex that will be ran against the host name.  If there is a match then the matcher accepts the uri.
     */
    protected String pathRegex = null;
    // CSON: VisibilityModifier

    @Override
    public final boolean accepts(final MatchInfo matchInfo) throws UnknownHostException, SocketException,
            MalformedURLException {
        Optional<Boolean> overridden = tryOverrideValidation(matchInfo);
        if (overridden.isPresent()) {
            return overridden.get();
        } else {
            int uriPort = matchInfo.getPort();
            if (uriPort != MatchInfo.ANY_PORT && this.port > 0 && uriPort != this.port) {
                return false;
            }

            if (this.pathRegex != null && matchInfo.getPath() != MatchInfo.ANY_PATH) {
                Matcher matcher = Pattern.compile(this.pathRegex).matcher(matchInfo.getPath());
                if (!matcher.matches()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * If the subclass has its own checks or if it has a different validation method this method can return a
     * valid value.
     *
     * @param matchInfo the match information to validate.
     */
    protected abstract Optional<Boolean> tryOverrideValidation(final MatchInfo matchInfo) throws UnknownHostException, SocketException,
            MalformedURLException;

    public final void setPort(final int port) {
        this.port = port;
    }

    /**
     * The regular expression used to verify the path of the uri as is expected.  A / will be added to the beginning of path if
     * it is missing because all paths start with /.
     * <p>
     * The regular expression used are the ones supported by java:
     * <a href="http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html">
     * http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
     * </a>
     * </p>
     *
     * @param pathRegex the regular expression.
     */
    public final void setPathRegex(final String pathRegex) {
        if (pathRegex.startsWith("/")) {
            this.pathRegex = pathRegex;
        } else {
            this.pathRegex = "/" + pathRegex;
        }
    }

    @Override
    public abstract String toString();

    // CHECKSTYLE:OFF
    // Don't run checkstyle on generated methods
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pathRegex == null) ? 0 : pathRegex.hashCode());
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HostMatcher other = (HostMatcher) obj;
        if (pathRegex == null) {
            if (other.pathRegex != null) {
                return false;
            }
        } else if (!pathRegex.equals(other.pathRegex)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        return true;
    }
    // CHECKSTYLE:ON

}
