/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class implementing some custom XPath functions.
 */
public class CustomXPath {
    /**
     * Takes a string with integers separated by ',' and returns this same
     * string but with the integers multiplied by the factor.
     */
    public String factorArray(String valsTxt, int factor) {
        String[] vals = valsTxt.split("[,]\\s*");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < vals.length; ++i) {
            String val = vals[i];
            if (i > 0) {
                result.append(",");
            }
            result.append(factorValue(val, factor));
        }

        return result.toString();
    }


    private static final Pattern NUMBER_UNIT = Pattern.compile("^(\\d*(\\.\\d*)?)(.*)$");

    public String factorValue(String valTxt, int factor) {
        Matcher matcher = NUMBER_UNIT.matcher(valTxt);
        if (matcher.matches()) {
            final String s = matcher.group(1);
            String txt = String.valueOf(Float.parseFloat(s) * factor);
            if (txt.endsWith(".0")) {
                txt = txt.substring(0, txt.length() - 2);
            }
            return txt + matcher.group(3);
        } else {
            throw new NumberFormatException("Cannot parse [" + valTxt + "]");
        }
    }
}
