package org.mapfish.print.processor.http.matcher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mapfish.print.processor.http.matcher.MatcherTestUtils.assertMatch;

import java.net.URI;
import org.apache.hc.client5.http.auth.AuthScope;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

public class DnsHostMatcherTest {

  @Test
  public void testAccepts() throws Exception {
    final DnsHostMatcher dnsHostMatcher = new DnsHostMatcher();
    dnsHostMatcher.setHost("localhost");

    assertMatch(
        dnsHostMatcher, true, new URI("http://127.0.0.1:8080/print-servlet"), HttpMethod.GET);
    assertMatch(
        dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.GET);
    assertMatch(
        dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.POST);
    assertMatch(dnsHostMatcher, true, new URI("http://localhost:90/print-servlet"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, true, new URI("http://localhost/print-servlet"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, true, new URI("https://localhost/print-servlet"), HttpMethod.GET);
    assertMatch(
        dnsHostMatcher, false, new URI("https://www.camptocamp.com/print-servlet"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, false, new URI("https://127.1.1.1/print-servlet"), HttpMethod.GET);
    assertTrue(
        dnsHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME, MatchInfo.ANY_HOST, 80, MatchInfo.ANY_REALM, "http"))));
    assertTrue(
        dnsHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME,
                    "localhost",
                    MatchInfo.ANY_PORT,
                    MatchInfo.ANY_REALM,
                    "http"))));
    assertTrue(
        dnsHostMatcher.matches(
            MatchInfo.fromAuthScope(
                new AuthScope(
                    MatchInfo.ANY_SCHEME,
                    "127.0.0.1",
                    80,
                    MatchInfo.ANY_REALM,
                    MatchInfo.ANY_SCHEME))));

    dnsHostMatcher.setPort(8080);

    assertMatch(
        dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.GET);
    assertMatch(
        dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.POST);
    assertMatch(
        dnsHostMatcher, false, new URI("http://localhost:90/print-servlet"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, false, new URI("http://localhost/print-servlet"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, false, new URI("https://localhost/print-servlet"), HttpMethod.GET);
    assertMatch(
        dnsHostMatcher,
        false,
        new URI("https://www.camptocamp.com:8080/print-servlet"),
        HttpMethod.GET);

    dnsHostMatcher.setPort(-1);
    dnsHostMatcher.setPathRegex("^/print.*");

    assertMatch(
        dnsHostMatcher, true, new URI("http://localhost:8080/print-servlet"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print-servlet"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, true, new URI("http://localhost:80/print"), HttpMethod.GET);
    assertMatch(
        dnsHostMatcher, true, new URI("http://localhost:80/print/anotherpath"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, false, new URI("http://localhost:80/pdf"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, false, new URI("http://localhost:80"), HttpMethod.GET);
    assertMatch(
        dnsHostMatcher, false, new URI("http://www.camptocamp.com:80/print"), HttpMethod.GET);
    assertMatch(dnsHostMatcher, false, new URI("http://www.camptocamp.com:80"), HttpMethod.GET);
  }
}
