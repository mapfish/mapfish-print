package org.mapfish.print.config.access;

import org.junit.Test;

public class AlwaysAllowAssertionTest {

    @Test
    public void testAssertAccess() {
        // as long as not error then we are good
        AlwaysAllowAssertion.INSTANCE.assertAccess("blah", this);
    }
}
