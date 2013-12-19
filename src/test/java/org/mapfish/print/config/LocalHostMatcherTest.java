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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.junit.Test;
import org.mapfish.print.PrintTestCase;

public class LocalHostMatcherTest extends PrintTestCase {
    @Test
    public void testAllIpV4() throws UnknownHostException, SocketException, URISyntaxException, MalformedURLException {
        LocalHostMatcher matcher = new LocalHostMatcher();

        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface networkInterface = ifaces.nextElement();

            Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress inetAddress = addrs.nextElement();
                if (inetAddress instanceof Inet4Address) {
                    final URI uri = new URI("http://" + inetAddress.getCanonicalHostName() + "/cgi-bin/mapserv");
                    assertTrue("testing " + uri, matcher.validate(uri));
                }
            }
        }

        assertFalse(matcher.validate(new URI("http://www.google.com/")));
    }
}
