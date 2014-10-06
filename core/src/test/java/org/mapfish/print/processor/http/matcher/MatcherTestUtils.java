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

import org.springframework.http.HttpMethod;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

/**
 * Support methods for the tests
 * @author Jesse on 9/5/2014.
 */
public class MatcherTestUtils {
    static void assertMatch(final URIMatcher matcher, boolean expected, final URI uri, final HttpMethod method)
            throws SocketException, UnknownHostException, MalformedURLException {
        assertEquals(expected, matcher.accepts(MatchInfo.fromUri(uri, method)));
    }
}
