package org.mapfish.print.jasperreports;

import java.util.Stack;

/** Used to convert an integer index to an alpha index. Index in: A..Z,AA..ZZ,AAA... */
public final class HumanAlphaSerie {

  private HumanAlphaSerie() {
    // Raise exception because the class should not be instantiated
    throw new AssertionError();
  }

  /**
   * Convert an integer to an alpha index.
   *
   * @param nbr the number to convert
   * @return the alpha index
   */
  public static String toString(final int nbr) {
    if (nbr <= 0) {
      return "";
    }

    // We want to start with A (1)
    int number = nbr + 1;

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

  /** Convert the stack to a string. */
  private static String convertToString(final Stack<Integer> array) {
    StringBuilder sb = new StringBuilder();

    while (!array.isEmpty()) {
      int number = array.pop();
      sb.append(numberToChar(number));
    }
    return sb.toString();
  }

  /** Convert a number to a char. 1 -> A, 2 -> B, 26 -> Z. */
  private static char numberToChar(final int number) {
    if (number > 26 || number < 0) {
      return '\0';
    }
    char c = (char) ('A' + number - 1);
    return c;
  }
}
