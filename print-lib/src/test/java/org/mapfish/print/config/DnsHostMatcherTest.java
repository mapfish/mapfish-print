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

import org.mapfish.print.PrintTestCase;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class DnsHostMatcherTest extends PrintTestCase {
    public DnsHostMatcherTest(String name) {
        super(name);
    }

    public void testSimple() throws URISyntaxException, UnknownHostException, SocketException, MalformedURLException {
        DnsHostMatcher matcher = new DnsHostMatcher();
        matcher.setHost("www.example.com");

        assertTrue(matcher.validate(new URI("http://www.example.com:80/toto")));
        assertTrue(matcher.validate(new URI("http://www.example.com:8000/toto")));
        assertFalse(matcher.validate(new URI("http://www.microsoft.com:80/toto")));
    }
}
