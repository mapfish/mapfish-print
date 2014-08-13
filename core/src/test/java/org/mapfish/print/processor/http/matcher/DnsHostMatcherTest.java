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

import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DnsHostMatcherTest {

    @Test
    public void testAccepts() throws Exception {
        final DnsHostMatcher dnsHostMatcher = new DnsHostMatcher();
        dnsHostMatcher.setHost("localhost");

        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:8080/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:8080/print-servlet"), HttpMethod.POST));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:90/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("https://localhost/print-servlet"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("https://www.camptocamp.com/print-servlet"), HttpMethod.GET));

        dnsHostMatcher.setPort(8080);

        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:8080/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:8080/print-servlet"), HttpMethod.POST));
        assertFalse(dnsHostMatcher.accepts(new URI("http://localhost:90/print-servlet"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://localhost/print-servlet"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("https://localhost/print-servlet"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("https://www.camptocamp.com:8080/print-servlet"), HttpMethod.GET));

        dnsHostMatcher.setPort(-1);
        dnsHostMatcher.setPathRegex("/print.*");

        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:8080/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:80/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:80/print"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:80/print/anotherpath"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://localhost:80/pdf"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://localhost:80"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://www.camptocamp.com:80/print"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://www.camptocamp.com:80"), HttpMethod.GET));

        dnsHostMatcher.setPathRegex("print.*");

        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:8080/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:80/print-servlet"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:80/print"), HttpMethod.GET));
        assertTrue(dnsHostMatcher.accepts(new URI("http://localhost:80/print/anotherpath"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://localhost:80/pdf"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://localhost:80"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://www.camptocamp.com:80/print"), HttpMethod.GET));
        assertFalse(dnsHostMatcher.accepts(new URI("http://www.camptocamp.com:80"), HttpMethod.GET));
    }
}