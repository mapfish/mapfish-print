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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class AddressHostMatcherTest extends PrintTestCase {
    public AddressHostMatcherTest(String name) {
        super(name);
    }

    public void testSimple() throws UnknownHostException, URISyntaxException, SocketException, MalformedURLException {
        AddressHostMatcher matcher = new AddressHostMatcher();
        matcher.setIp("127.2.3.56");
        matcher.setMask("255.255.255.127");

        assertTrue(matcher.validate(new URI("http://127.2.3.56/cgi-bin/mapserv?toto=tutu")));
        assertTrue(matcher.validate(new URI("http://127.2.3.184/cgi-bin/mapserv?toto=tutu")));
        assertFalse(matcher.validate(new URI("http://127.2.3.156/cgi-bin/mapserv?toto=tutu")));
    }

    public void testDns() throws UnknownHostException, URISyntaxException, SocketException, MalformedURLException {
        AddressHostMatcher matcher = new AddressHostMatcher();
        matcher.setIp("localhost");
        matcher.setPort(80);
        matcher.setMask("255.255.255.255");

        assertTrue(matcher.validate(new URI("http://127.0.0.1/cgi-bin/mapserv?toto=tutu")));
    }

    public void testPort() throws UnknownHostException, URISyntaxException, SocketException, MalformedURLException {
        AddressHostMatcher matcher = new AddressHostMatcher();
        matcher.setIp("localhost");
        matcher.setPort(180);
        matcher.setMask("255.255.255.255");

        assertTrue(matcher.validate(new URI("http://127.0.0.1:180/cgi-bin/mapserv?toto=tutu")));
        assertFalse(matcher.validate(new URI("http://127.0.0.1/cgi-bin/mapserv?toto=tutu")));
        assertFalse(matcher.validate(new URI("http://127.0.0.1:80/cgi-bin/mapserv?toto=tutu")));
    }

    public void testPath() throws UnknownHostException, URISyntaxException, SocketException, MalformedURLException {
        AddressHostMatcher matcher = new AddressHostMatcher();
        matcher.setIp("127.0.0.1");
        matcher.setPathRegex("^/cgi-bin/mapserv$");

        assertTrue(matcher.validate(new URI("http://127.0.0.1/cgi-bin/mapserv?toto=tutu")));
        assertTrue(matcher.validate(new URI("http://127.0.0.1:80/cgi-bin/mapserv")));
        assertFalse(matcher.validate(new URI("http://127.0.0.1/cgi-bin/mapserv/titi")));
        assertFalse(matcher.validate(new URI("http://127.0.0.2/cgi-bin/mapserv?toto=tutu")));
    }

    public void testMulti() throws UnknownHostException, URISyntaxException, SocketException, MalformedURLException {
        AddressHostMatcher matcher = new AddressHostMatcher();
        matcher.setIp("www.example.com");
        matcher.setMask("255.255.255.255");
        matcher.buildMaskedAuthorizedIPs(new InetAddress[]{
                InetAddress.getByName("10.1.0.1"),
                InetAddress.getByName("10.1.1.1")
        });

        assertTrue(matcher.validate(new URI("http://10.1.0.1/cgi-bin/mapserv?toto=tutu")));
        assertTrue(matcher.validate(new URI("http://10.1.1.1/cgi-bin/mapserv?toto=tutu")));
        assertFalse(matcher.validate(new URI("http://10.1.2.1/cgi-bin/mapserv?toto=tutu")));
    }
}
