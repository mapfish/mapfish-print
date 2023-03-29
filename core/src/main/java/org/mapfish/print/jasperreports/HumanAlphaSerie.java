package org.mapfish.print;

import java.util.Stack;

/**
 * Used to convert an integer index to an alpha index. Index in: A..Z,AA..ZZ,AAA...
 */
public class HumanAlphaSerie {

    public static String toString(int number) {
        if (number <= 0) {
            return "";
        }

        // We want to start with A (1)
        number += 1;

        Stack<Integer> stack = new Stack<>();
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
        char c = (char) ('A' + number - 1);
        return c;
    }
}
