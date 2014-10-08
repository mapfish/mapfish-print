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

import org.apache.http.auth.AuthScope;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.junit.Assert.assertTrue;
import static org.mapfish.print.processor.http.matcher.MatcherTestUtils.*;

public class DnsHostMatcherTest {

    @Test
    public void testAccepts() throws Exception {
        final DnsHostMatcher dnsHostMatcher = new DnsHostMatcher();
        dnsHostMatcher.setHost("localhost");

        assertMatch(dnsHostMatcher, true, new URI("http://127.0.0.1:8080/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.POST);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:90/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("https://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("https://www.camptocamp.com/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("https://127.1.1.1/print-servlet"), HttpMethod.GET);
        assertTrue(dnsHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope(AuthScope.ANY_HOST, 80, AuthScope.ANY_REALM, "http"))));
        assertTrue(dnsHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM, "http"))));
        assertTrue(dnsHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", 80, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME))));

        dnsHostMatcher.setPort(8080);

        assertMatch(dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.POST);
        assertMatch(dnsHostMatcher, false, new URI("http://localhost:90/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("https://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("https://www.camptocamp.com:8080/print-servlet"), HttpMethod.GET);

        dnsHostMatcher.setPort(-1);
        dnsHostMatcher.setPathRegex("/print.*");

        assertMatch(dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print/anotherpath"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://localhost:80/pdf"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://localhost:80"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://www.camptocamp.com:80/print"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://www.camptocamp.com:80"), HttpMethod.GET);

        dnsHostMatcher.setPathRegex("print.*");

        assertMatch(dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print-servlet"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print/anotherpath"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://localhost:80/pdf"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://localhost:80"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://www.camptocamp.com:80/print"), HttpMethod.GET);
        assertMatch(dnsHostMatcher, false, new URI("http://www.camptocamp.com:80"), HttpMethod.GET);
    }
}