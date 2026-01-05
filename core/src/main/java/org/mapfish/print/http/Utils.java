package org.mapfish.print.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;

/** Network utils class. */
public final class Utils {
  private Utils() {}

  private static final List<String> AUTH_HEADERS =
      Arrays.asList("cookie", "set-cookie", "authorization", "x-csrf-token");

  /**
   * Get a list of printable (auth header will be hidden) headers 'header=value'.
   *
   * @param headers The headers
   */
  public static List<String> getPrintableHeadersList(final HttpHeaders headers) {
    final List<String> result = new ArrayList<>();
    for (String header : headers.headerNames()) {
      List<String> value = headers.get(header);
      if (AUTH_HEADERS.contains(header.toLowerCase())) {
        value = Arrays.asList(new String[] {"***"});
      }
      result.add(String.format("%s: %s", header, String.join(", ", value)));
    }
    return result;
  }
}
