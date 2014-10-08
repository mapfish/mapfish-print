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

public class AddressHostMatcherTest {

    @Test
    public void testAccepts() throws Exception {
        final AddressHostMatcher addressHostMatcher = new AddressHostMatcher();
        addressHostMatcher.setIp("127.0.0.1");

        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1"), HttpMethod.GET);
        assertMatch(addressHostMatcher, false, new URI("http://127.0.1.1"), HttpMethod.GET);
        assertTrue(addressHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope(AuthScope.ANY_HOST, 80, AuthScope.ANY_REALM, "http"))));
        assertTrue(addressHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", AuthScope.ANY_PORT, AuthScope.ANY_REALM, "http"))));
        assertTrue(addressHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", 80, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME))));

        addressHostMatcher.setMask("255.255.255.0");
        addressHostMatcher.setIp("127.0.0.0");

        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1"), HttpMethod.GET);
        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1"), HttpMethod.POST);
        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.2"), HttpMethod.GET);
        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.3"), HttpMethod.GET);
        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.4"), HttpMethod.GET);
        assertMatch(addressHostMatcher, false, new URI("http://127.0.1.1"), HttpMethod.GET);
        assertTrue(addressHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope(AuthScope.ANY_HOST, 80, AuthScope.ANY_REALM, "http"))));
        assertTrue(addressHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", AuthScope.ANY_PORT, AuthScope.ANY_REALM, "http"))));
        assertTrue(addressHostMatcher.accepts(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", 80, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME))));

        addressHostMatcher.setPort(8080);
        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1:8080"), HttpMethod.GET);
        assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:80"), HttpMethod.GET);
        assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1"), HttpMethod.GET);

        addressHostMatcher.setPort(-1);
        addressHostMatcher.setPathRegex("/print/.+");
        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1:8080/print/create"), HttpMethod.GET);
        assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1:80/print/create"), HttpMethod.GET);
        assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:8080/print"), HttpMethod.GET);
        assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:8080/print/"), HttpMethod.GET);
        assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:8080/pdf"), HttpMethod.GET);
    }
}