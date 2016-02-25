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

import java.util.Stack;

/**
 * Used to convert an integer index to an alpha index.
 * Index in: A..Z,AA..ZZ,AAA...
 *
 * @author sbrunner
 */
public class HumanAlphaSerie {

    public static String toString(int number) {
        if (number <= 0) {
            return "";
        }

        // We want to start with A (1)
        number += 1;

        Stack<Integer> stack = new Stack<Integer>();
        while (number > 26) {
            int remain = number % 26;
            if (remain == 0) {
                remain = 26;
            }

            stack.add(remain);
            number = number - remain;
            number = number / 26;
        }

        if (number != 0) {
            stack.add(number);
        }

        return convertToString(stack);
    }

    private static String convertToString(Stack<Integer> array) {
        StringBuilder sb = new StringBuilder();

        while (!array.isEmpty()) {
            int number = array.pop();
            sb.append(numberToChar(number));
        }
        return sb.toString();
    }

    private static char numberToChar(int number) {
        if (number > 26 || number < 0) {
            return '\0';
        }
        char c = (char)('A' + number - 1);
        return c;
    }
}
