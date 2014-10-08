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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsServer;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.processor.http.matcher.DnsHostMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        AbstractMapfishSpringTest.DEFAULT_SPRING_XML,
        "classpath:/org/mapfish/print/http/proxy/application-context-proxy-test.xml"
})
public class HttpCredentialTest {
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final int HTTPS_PROXY_PORT = 21433;
    private static HttpsServer httpsServer;

    @Autowired
    ConfigurationFactory configurationFactory;
    @Autowired
    private MfClientHttpRequestFactoryImpl requestFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        httpsServer = HttpProxyTest.createHttpsServer(HTTPS_PROXY_PORT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        httpsServer.stop(0);
    }

    @Test
    public void testValidate() throws Exception {
        final HttpCredential credential = new HttpCredential();
        Configuration configuration = new Configuration();

        List<Throwable> errors = Lists.newArrayList();
        credential.validate(errors, configuration);
        assertEquals(1, errors.size());

        errors.clear();
        credential.validate(errors, configuration);
        assertEquals(1, errors.size());

        credential.setUsername("username");

        errors.clear();
        credential.validate(errors, configuration);
        assertEquals(0, errors.size());
    }

    @Test
    public void testToCredentials() throws Exception {
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
        assertNotNull(credential.toCredentials(authscope));

        authscope = new AuthScope("google.com", AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
        assertNull(credential.toCredentials(authscope));

        authscope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, "http");
        assertNotNull(credential.toCredentials(authscope));
    }

    @Test
    public void testToHttpsBehaviour() throws Exception {
        final String message = "Message from server";

        final String path = "/username";
        httpsServer.createContext(path, new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                final String authorization = httpExchange.getRequestHeaders().getFirst("Authorization");
                if (authorization == null) {
                    httpExchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"Test Site\"");
                    httpExchange.sendResponseHeaders(401, 0);
                    httpExchange.close();
                } else {
                    final String expectedAuth = "Basic dXNlcm5hbWU6cGFzc3dvcmQ=";
                    if (authorization.equals(expectedAuth)) {
                        HttpProxyTest.respond(httpExchange, message, 200);
                    } else {
                        final String errorMessage = "Expected authorization:\n'" + expectedAuth + "' but got:\n'" + authorization + "'";
                        HttpProxyTest.respond(httpExchange, errorMessage, 500);
                    }
                }
            }
        });

        final HttpCredential credential = new HttpCredential();
        credential.setUsername(USERNAME);
        credential.setPassword(PASSWORD);

        final DnsHostMatcher matcher = new DnsHostMatcher();
        matcher.setHost(HttpProxyTest.LOCALHOST);
        credential.setMatchers(Collections.singletonList(matcher));

        final String target = "https://" + HttpProxyTest.LOCALHOST + ":" + HTTPS_PROXY_PORT;
        HttpProxyTest.assertCorrectResponse(this.configurationFactory, this.requestFactory, credential, message, target, path);
    }
}