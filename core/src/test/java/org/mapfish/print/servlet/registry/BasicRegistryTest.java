package org.mapfish.print.servlet.registry;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class BasicRegistryTest extends AbstractMapfishSpringTest {

    @Autowired
    private Registry registry;

    @Test
    public void testIncrementLong() {
        final String key = "key1";
        registry.incrementLong(key, 2L);
        assertEquals(2L, registry.getNumber(key));
        registry.incrementLong(key, 2L);
        assertEquals(4L, registry.getNumber(key));
    }

    @Test
    public void testIncrementInt() {
        final String key = "key2";
        registry.incrementInt(key, 2);
        assertEquals(2, registry.getNumber(key));
        registry.incrementInt(key, 2);
        assertEquals(4, registry.getNumber(key));
    }

}
