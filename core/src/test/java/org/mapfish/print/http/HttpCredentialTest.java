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

package org.mapfish.print.http;

import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mapfish.print.processor.http.matcher.DnsHostMatcher;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HttpCredentialTest {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static HttpsServer httpsServer;

    @BeforeClass
    public static void setUp() throws Exception {
        httpsServer = HttpProxyTest.createHttpsServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        httpsServer.stop(0);
    }

    @Test
    public void testValidate() throws Exception {
        final HttpCredential credential = new HttpCredential();

        List<Throwable> errors = Lists.newArrayList();
        credential.validate(errors);
        assertEquals(1, errors.size());

        errors.clear();
        credential.validate(errors);
        assertEquals(1, errors.size());

        credential.setUsername("username");

        errors.clear();
        credential.validate(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void testToCredentials() throws Exception {
        final String path = "/credentials";
        final String message = "message from server";

        final HttpCredential credential = new HttpCredential();
        credential.setUsername(USERNAME);
        credential.setPassword(PASSWORD);

        final DnsHostMatcher matcher = new DnsHostMatcher();
        matcher.setHost(HttpProxyTest.LOCALHOST);
        credential.setMatchers(Collections.singletonList(matcher));

        AuthScope authscope = AuthScope.ANY;
        final Credentials object = credential.toCredentials(authscope);
        assertNotNull(object);
        assertEquals(USERNAME, object.getUserPrincipal().getName());
        assertEquals(PASSWORD, object.getPassword());

        authscope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
        assertNotNull(credential.toCredentials(authscope));

        authscope = new AuthScope(AuthScope.ANY_HOST, HttpProxyTest.HTTPS_PROXY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
        assertNotNull(credential.toCredentials(authscope));

        authscope = new AuthScope(AuthScope.ANY_HOST, 80, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
        assertNull(credential.toCredentials(authscope));

        authscope = new AuthScope("google.com", AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
        assertNull(credential.toCredentials(authscope));

        authscope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, "http");
        assertNull(credential.toCredentials(authscope));
    }
}