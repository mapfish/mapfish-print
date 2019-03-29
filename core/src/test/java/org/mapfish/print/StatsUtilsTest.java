package org.mapfish.print;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatsUtilsTest {
    @Test
    public void testQuotePart() {
        assertEquals("toto_tutu_titi", StatsUtils.quotePart("toto.tutu:titi"));
        assertEquals("NULL", StatsUtils.quotePart(null));
    }
}
