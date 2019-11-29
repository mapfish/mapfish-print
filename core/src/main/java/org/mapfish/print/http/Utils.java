package org.mapfish.print.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Network utils classe.
 */
public final class Utils {
    private Utils() {
    }

    private static final List<String> AUTH_HEADERS = Arrays.asList(
        new String[]{"cookie", "set-cookie", "authorization", "x-csrf-token"}
    );

    /**
     * Get a list of printable (auth header will be hidden) headers 'header=value'.
     *
     * @param headers The headers
     */
    public static List<String> getPrintableHeadersList(final Map<String, List<String>> headers) {
        final List<String> result = new ArrayList<String>();
        for (String header: headers.keySet()) {
            List<String> value = headers.get(header);
            if (AUTH_HEADERS.contains(header.toLowerCase())) {
                value = Arrays.asList(new String[]{"***"});
            }
            result.add(String.format("%s: %s", header, String.join(", ", value)));
        }
        return result;
    }
}
