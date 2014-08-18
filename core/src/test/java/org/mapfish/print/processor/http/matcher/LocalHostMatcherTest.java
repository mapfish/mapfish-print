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

import org.junit.Test;
import org.springframework.http.HttpMethod;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalHostMatcherTest {
    @Test
    public void testAccepts() throws Exception {
        final LocalHostMatcher localHostMatcher = new LocalHostMatcher();

        final InetAddress[] localhosts = InetAddress.getAllByName("localhost");
        for (InetAddress localhost : localhosts) {
            assertTrue(localHostMatcher.accepts(new URI("http://"+localhost.getHostName()), HttpMethod.GET));
            assertTrue(localHostMatcher.accepts(new URI("https://"+localhost.getHostName()), HttpMethod.GET));
            assertTrue(localHostMatcher.accepts(new URI("https://"+localhost.getHostName()), HttpMethod.POST));
            assertTrue(localHostMatcher.accepts(new URI("http://"+localhost.getHostName()), HttpMethod.POST));
            assertTrue(localHostMatcher.accepts(new URI("http://"+localhost.getHostName()), HttpMethod.HEAD));
            assertTrue(localHostMatcher.accepts(new URI("http://"+localhost.getHostName() + "/print/create"), HttpMethod.GET));
            assertTrue(localHostMatcher.accepts(new URI("http://"+localhost.getHostName() + ":8080"), HttpMethod.GET));
        }

        assertFalse(localHostMatcher.accepts(new URI("http://www.camptocamp.com/"), HttpMethod.GET));

        localHostMatcher.setPort(8080);

        for (InetAddress localhost : localhosts) {
            assertTrue(localHostMatcher.accepts(new URI("http://"+localhost.getHostName() + ":8080"), HttpMethod.GET));
            assertFalse(localHostMatcher.accepts(new URI("http://"+localhost.getHostName()), HttpMethod.GET));
        }

        assertFalse(localHostMatcher.accepts(new URI("http://www.camptocamp.com:8080/"), HttpMethod.GET));

        localHostMatcher.setPort(-1);
        localHostMatcher.setPathRegex("/print/.+");
        for (InetAddress localhost : localhosts) {
            assertTrue(localHostMatcher.accepts(new URI("http://"+localhost.getHostName() + "/print/create"), HttpMethod.GET));
            assertFalse(localHostMatcher.accepts(new URI("http://"+localhost.getHostName() + "/printing/create"), HttpMethod.GET));
        }

        assertFalse(localHostMatcher.accepts(new URI("http://www.camptocamp.com/print/create"), HttpMethod.GET));
    }


}