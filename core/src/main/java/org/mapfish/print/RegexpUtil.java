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

package org.mapfish.print;

import java.util.regex.Pattern;

/**
 * Regular Expression utilities.
 *
 * @author Jesse on 6/25/2014.
 */
public final class RegexpUtil {
    private RegexpUtil() {
        // intentionally empty
    }

    /**
     * Convert a string to a Pattern object.
     * <ul>
     *     <li>If the host starts and ends with / then it is compiled as a regular expression</li>
     *     <li>Otherwise the hosts must exactly match</li>
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
