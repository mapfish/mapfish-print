package org.mapfish.print.processor.http.matcher;

import org.apache.http.auth.AuthScope;
import org.junit.Test;
import org.springframework.http.HttpMethod;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;
import static org.mapfish.print.processor.http.matcher.MatcherTestUtils.assertMatch;

public class LocalHostMatcherTest {
    private final LocalHostMatcher localHostMatcher = new LocalHostMatcher();

    @Test
    public void testAccepts() throws Exception {

        final InetAddress[] localhosts = InetAddress.getAllByName("localhost");
        for (InetAddress localhost: localhosts) {

            assertMatch(localHostMatcher, true, new URI("http://" + localhost.getHostName()), HttpMethod.GET);
            assertMatch(localHostMatcher, true, new URI("https://" + localhost.getHostName()),
                        HttpMethod.GET);
            assertMatch(localHostMatcher, true, new URI("https://" + localhost.getHostName()),
                        HttpMethod.POST);
            assertMatch(localHostMatcher, true, new URI("http://" + localhost.getHostName()),
                        HttpMethod.POST);
            assertMatch(localHostMatcher, true, new URI("http://" + localhost.getHostName()),
                        HttpMethod.HEAD);
            assertMatch(localHostMatcher, true,
                        new URI("http://" + localhost.getHostName() + "/print/create"), HttpMethod.GET);
            assertMatch(localHostMatcher, true, new URI("http://" + localhost.getHostName() + ":8080"),
                        HttpMethod.GET);
        }


        assertTrue(localHostMatcher.matches(MatchInfo.fromAuthScope(
                new AuthScope(AuthScope.ANY_HOST, 80, AuthScope.ANY_REALM, "http"))));
        assertTrue(localHostMatcher.matches(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", AuthScope.ANY_PORT, AuthScope.ANY_REALM, "http"))));
        assertTrue(localHostMatcher.matches(MatchInfo.fromAuthScope(
                new AuthScope("127.0.0.1", 80, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME))));

        assertMatch(localHostMatcher, false, new URI("http://www.camptocamp.com/"), HttpMethod.GET);

        localHostMatcher.setPort(8080);

        for (InetAddress localhost: localhosts) {
            assertMatch(localHostMatcher, true, new URI("http://" + localhost.getHostName() + ":8080"),
                        HttpMethod.GET);
            assertMatch(localHostMatcher, false, new URI("http://" + localhost.getHostName()),
                        HttpMethod.GET);
        }

        assertMatch(localHostMatcher, false, new URI("http://www.camptocamp.com:8080/"), HttpMethod.GET);

        localHostMatcher.setPort(-1);
        localHostMatcher.setPathRegex("/print/.+");
        for (InetAddress localhost: localhosts) {
            assertMatch(localHostMatcher, true,
                        new URI("http://" + localhost.getHostName() + "/print/create"), HttpMethod.GET);
            assertMatch(localHostMatcher, false,
                        new URI("http://" + localhost.getHostName() + "/printing/create"), HttpMethod.GET);
        }

        assertMatch(localHostMatcher, false, new URI("http://www.camptocamp.com/print/create"),
                    HttpMethod.GET);
    }

    @Test
    public void testLoopback()
            throws URISyntaxException, SocketException, UnknownHostException, MalformedURLException {
        assertMatch(localHostMatcher, true, new URI("http://127.0.0.1/"), HttpMethod.GET);
        assertMatch(localHostMatcher, true, new URI("http://127.36.0.0/"), HttpMethod.GET);
        assertMatch(localHostMatcher, false, new URI("http://128.0.0.0/"), HttpMethod.GET);
    }


}
