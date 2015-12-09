/*
 * Copyright (C) 2015  Camptocamp
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

import org.springframework.http.HttpMethod;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * Hold a list of {@link URIMatcher} and implement the logic to see if any matches an URI.
 */
public final class UriMatchers {
    private List<? extends URIMatcher> matchers = Collections.singletonList(new AcceptAllMatcher());

    /**
     * Set the matchers.
     * @param matchers the new list.
     */
    public void setMatchers(final List<? extends URIMatcher> matchers) {
        this.matchers = matchers;
    }

    /**
     * @param uri the URI to create a request for
     * @param httpMethod the HTTP method to execute
     * @return true if it's matching.
     */
    public boolean matches(final URI uri, final HttpMethod httpMethod)
            throws SocketException, UnknownHostException, MalformedURLException {
        for (URIMatcher matcher : this.matchers) {
            if (matcher.matches(MatchInfo.fromUri(uri, httpMethod))) {
                return !matcher.isReject();
            }
        }
        return false;
    }

    /**
     * Validate the configuration.
     * @param validationErrors where to put the errors.
     */
    public void validate(final List<Throwable> validationErrors) {
        if (this.matchers == null) {
            validationErrors.add(new IllegalArgumentException(
                    "Matchers cannot be null.  There should be at least a !acceptAll matcher"));
        }
        if (this.matchers != null && this.matchers.isEmpty()) {
            validationErrors.add(new IllegalArgumentException(
                    "There are no url matchers defined.  There should be at least a " +
                    "!acceptAll matcher"));
        }
    }
}
