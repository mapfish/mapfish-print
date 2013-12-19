/*
 * Copyright (C) 2013  Camptocamp
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

import static org.junit.Assert.*;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.junit.Test;
import org.mapfish.print.PrintTestCase;

public class DnsHostMatcherTest extends PrintTestCase {
    @Test
    public void testSimple() throws URISyntaxException, UnknownHostException, SocketException, MalformedURLException {
        DnsHostMatcher matcher = new DnsHostMatcher();
        matcher.setHost("www.example.com");

        assertTrue(matcher.validate(new URI("http://www.example.com:80/toto")));
        assertTrue(matcher.validate(new URI("http://www.example.com:8000/toto")));
        assertFalse(matcher.validate(new URI("http://www.microsoft.com:80/toto")));
    }
}
