package org.mapfish.print;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StatsUtilsTest {
  @Test
  public void testQuotePart() {
    assertEquals("toto_tutu_titi", StatsUtils.quotePart("toto.tutu:titi"));
    assertEquals("NULL", StatsUtils.quotePart(null));
  }
}
