package org.mapfish.print.processor.http.matcher;

import org.apache.http.auth.AuthScope;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mapfish.print.processor.http.matcher.MatcherTestUtils.assertMatch;

/**
 * Test for {@link HostnameMatcher}
 */
public class HostnameMatcherTest {

    @Test
    public void testNoSubdomain() throws Exception {
        HostnameMatcher hostnameHostMatcher = new HostnameMatcher();
        hostnameHostMatcher.setHost("localhost");

        assertMatch(hostnameHostMatcher, false, new URI("http://127.0.0.1:8080/print-servlet"),
                    HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:8080/print-servlet"),
                    HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:8080/print-servlet"),
                    HttpMethod.POST);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:90/print-servlet"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("https://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("https://www.camptocamp.com/print-servlet"),
                    HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("https://127.1.1.1/print-servlet"), HttpMethod.GET);
        assertTrue(hostnameHostMatcher.matches(MatchInfo.fromAuthScope(
                new AuthScope(AuthScope.ANY_HOST, 80, AuthScope.ANY_REALM, "http"))));
        assertTrue(hostnameHostMatcher.matches(MatchInfo.fromAuthScope(
                new AuthScope("localhost", AuthScope.ANY_PORT, AuthScope.ANY_REALM, "http"))));
        assertFalse(hostnameHostMatcher.matches(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", 80, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME))));

        hostnameHostMatcher.setPort(8080);

        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:8080/print-servlet"),
                    HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:8080/print-servlet"),
                    HttpMethod.POST);
        assertMatch(hostnameHostMatcher, false, new URI("http://localhost:90/print-servlet"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("http://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("https://localhost/print-servlet"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("https://www.camptocamp.com:8080/print-servlet"),
                    HttpMethod.GET);

        hostnameHostMatcher.setPort(-1);
        hostnameHostMatcher.setPathRegex("^/print.*");

        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:8080/print-servlet"),
                    HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:80/print-servlet"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:80/print"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://localhost:80/print/anotherpath"),
                    HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("http://localhost:80/pdf"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("http://localhost:80"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("http://www.camptocamp.com:80/print"),
                    HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("http://www.camptocamp.com:80"), HttpMethod.GET);

        hostnameHostMatcher = new HostnameMatcher();
        hostnameHostMatcher.setHost("www.camptocamp.com");
        assertMatch(hostnameHostMatcher, true, new URI("http://www.camptocamp.com:80/print"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, true, new URI("http://www.camptocamp.com:80"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("http://intranet.camptocamp.com:80"), HttpMethod.GET);
        assertMatch(hostnameHostMatcher, false, new URI("http://xxx.www.camptocamp.com:80"), HttpMethod.GET);
    }

    @Test
    public void testSubDomain() throws Exception {
        HostnameMatcher matcher = new HostnameMatcher();
        matcher.setHost("example.com");
        matcher.setAllowSubDomains(true);
        assertMatch(matcher, true, new URI("http://example.com/print-servlet"), HttpMethod.GET);
        assertMatch(matcher, false, new URI("http://example2.com/print-servlet"), HttpMethod.GET);
        assertMatch(matcher, true, new URI("http://www.example.com/print-servlet"), HttpMethod.GET);
        assertMatch(matcher, false, new URI("http://www.example2.com/print-servlet"), HttpMethod.GET);
        assertMatch(matcher, false, new URI("http://com/print-servlet"), HttpMethod.GET);
    }
}
