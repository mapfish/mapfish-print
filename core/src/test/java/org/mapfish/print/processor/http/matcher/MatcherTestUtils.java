package org.mapfish.print.processor.http.matcher;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.http.HttpMethod;

/* Support methods for the tests */
public final class MatcherTestUtils {
  private MatcherTestUtils() {
    // should not be instantiated
  }

  static void assertMatch(
      final URIMatcher matcher, final boolean expected, final URI uri, final HttpMethod method)
      throws SocketException, UnknownHostException, MalformedURLException {
    assertEquals(expected, matcher.matches(MatchInfo.fromUri(uri, method)));
  }
}
