package org.mapfish.print;

import java.util.regex.Pattern;

/**
 * Regular Expression utilities.
 */
public final class RegexpUtil {
    private RegexpUtil() {
        // intentionally empty
    }

    /**
     * Convert a string to a Pattern object.
     * <ul>
     * <li>If the host starts and ends with / then it is compiled as a regular expression</li>
     * <li>Otherwise the hosts must exactly match</li>
     * </ul>
     *
     * @param expr the expression to compile
     */
    public static Pattern compilePattern(final String expr) {
        Pattern pattern;
        final int lastChar = expr.length() - 1;
        if (expr.charAt(0) == '/' && expr.charAt(lastChar) == '/') {
            pattern = Pattern.compile(expr.substring(1, lastChar));
        } else {
            pattern = Pattern.compile(Pattern.quote(expr));
        }
        return pattern;
    }
}
