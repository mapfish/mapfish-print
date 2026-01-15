package org.mapfish.print.processor.http.matcher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mapfish.print.processor.http.matcher.MatcherTestUtils.assertMatch;

import java.net.URI;
import org.apache.hc.client5.http.auth.AuthScope;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

public class AddressHostMatcherTest {

  @Test
  public void testAccepts() throws Exception {
    final AddressHostMatcher addressHostMatcher = new AddressHostMatcher();
    addressHostMatcher.setIp("127.0.0.1");

    assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1"), HttpMethod.GET);
    assertMatch(addressHostMatcher, false, new URI("http://127.0.1.1"), HttpMethod.GET);
    assertTrue(
        addressHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME, MatchInfo.ANY_HOST, 80, MatchInfo.ANY_REALM, "http"))));
    assertTrue(
        addressHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME,
                    "127.0.0.1",
                    MatchInfo.ANY_PORT,
                    MatchInfo.ANY_REALM,
                    "http"))));
    assertTrue(
        addressHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME,
                    "127.0.0.1",
                    80,
                    MatchInfo.ANY_REALM,
                    MatchInfo.ANY_SCHEME))));

    addressHostMatcher.setMask("255.255.255.0");
    addressHostMatcher.setIp("127.0.0.0");

    assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1"), HttpMethod.GET);
    assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1"), HttpMethod.POST);
    assertMatch(addressHostMatcher, true, new URI("http://127.0.0.2"), HttpMethod.GET);
    assertMatch(addressHostMatcher, true, new URI("http://127.0.0.3"), HttpMethod.GET);
    assertMatch(addressHostMatcher, true, new URI("http://127.0.0.4"), HttpMethod.GET);
    assertMatch(addressHostMatcher, false, new URI("http://127.0.1.1"), HttpMethod.GET);
    assertTrue(
        addressHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME, MatchInfo.ANY_HOST, 80, MatchInfo.ANY_REALM, "http"))));
    assertTrue(
        addressHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME,
                    "127.0.0.1",
                    MatchInfo.ANY_PORT,
                    MatchInfo.ANY_REALM,
                    "http"))));
    assertTrue(
        addressHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME,
                    "127.0.0.1",
                    80,
                    MatchInfo.ANY_REALM,
                    MatchInfo.ANY_SCHEME))));

    addressHostMatcher.setPort(8080);
    assertMatch(addressHostMatcher, true, new URI("http://127.0.0.1:8080"), HttpMethod.GET);
    assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:80"), HttpMethod.GET);
    assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1"), HttpMethod.GET);

    addressHostMatcher.setPort(-1);
    addressHostMatcher.setPathRegex("/print/.+");
    assertMatch(
        addressHostMatcher, true, new URI("http://127.0.0.1:8080/print/create"), HttpMethod.GET);
    assertMatch(
        addressHostMatcher, true, new URI("http://127.0.0.1:80/print/create"), HttpMethod.GET);
    assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:8080/print"), HttpMethod.GET);
    assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:8080/print/"), HttpMethod.GET);
    assertMatch(addressHostMatcher, false, new URI("http://127.0.0.1:8080/pdf"), HttpMethod.GET);
  }
}
