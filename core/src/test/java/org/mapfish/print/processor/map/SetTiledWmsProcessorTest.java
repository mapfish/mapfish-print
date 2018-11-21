package org.mapfish.print.processor.map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SetTiledWmsProcessorTest {
    @Test
    public void testAdaptTileDimension() {
        assertEquals(3, SetTiledWmsProcessor.adaptTileDimension(7, 3));
        assertEquals(3, SetTiledWmsProcessor.adaptTileDimension(6, 3));
        assertEquals(5, SetTiledWmsProcessor.adaptTileDimension(20, 6));
        assertEquals(5, SetTiledWmsProcessor.adaptTileDimension(19, 6));
        assertEquals(6, SetTiledWmsProcessor.adaptTileDimension(18, 6));
        assertEquals(200, SetTiledWmsProcessor.adaptTileDimension(600, 255));
    }
}
