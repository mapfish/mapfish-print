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

package org.mapfish.print.config.layout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bean for configuring a cell's borders, paddings and background color.
 * Includes the mean to put rules to specify to what cell this configuration applys.
 * <p/>
 * See http://trac.mapfish.org/trac/mapfish/wiki/PrintModuleServer#Tableconfiguration
 */
public class CellException extends CellConfig {
    private CoordMatcher row;
    private CoordMatcher col;

    public boolean matches(int row, int col) {
        return (this.row == null || this.row.matches(row)) &&
                (this.col == null || this.col.matches(col));
    }

    public void setRow(String row) {
        this.row = createMatcher(row);
    }

    public void setCol(String col) {
        this.col = createMatcher(col);
    }

    private static final Pattern SIMPLE = Pattern.compile("^\\d+$");
    private static final Pattern RANGE = Pattern.compile("^(\\d+)-(\\d+)$");

    private CoordMatcher createMatcher(String value) {
        Matcher simple = SIMPLE.matcher(value);
        if (simple.matches()) {
            return new IntCoordMatcher(Integer.parseInt(value));
        } else {
            Matcher range = RANGE.matcher(value);
            if (range.matches()) {
                return new IntCoordMatcher(Integer.parseInt(range.group(1)), Integer.parseInt(range.group(2)));
            } else {
                return new RegExpMatcher(value);
            }
        }
    }

    public static interface CoordMatcher {
        public boolean matches(int pos);
    }

    public static class IntCoordMatcher implements CoordMatcher {
        private int min;
        private int max;

        public IntCoordMatcher(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public IntCoordMatcher(int value) {
            this.min = value;
            this.max = value;
        }

        public boolean matches(int pos) {
            return pos >= min && pos <= max;
        }

        public String toString() {
            return "IntCoordMatcher{" +
                    "min=" + min +
                    ", max=" + max +
                    '}';
        }
    }

    public static class RegExpMatcher implements CoordMatcher {
        private Pattern regexp;

        public RegExpMatcher(String value) {
            regexp = Pattern.compile(value);
        }

        public boolean matches(int pos) {
            return regexp.matcher(Integer.toString(pos)).matches();
        }
    }
}
