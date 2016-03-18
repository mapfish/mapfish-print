package org.mapfish.print.attribute.map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Jesse on 4/1/14.
 */
public class ZoomLevelsTest {

    @Test
    public void testRemoveDuplicates() throws Exception {
        final ZoomLevels zoomLevels = new ZoomLevels(4, 2, 3, 3, 2, 1, 3, 2, 1);

        assertEquals(4, zoomLevels.size());
        assertEquals(4, (int) zoomLevels.get(0));
        assertEquals(3, (int) zoomLevels.get(1));
        assertEquals(2, (int) zoomLevels.get(2));
        assertEquals(1, (int) zoomLevels.get(3));

    }
    @Test
    public void testSort() throws Exception {

        final ZoomLevels zoomLevels = new ZoomLevels(4,2,3,1);

        assertEquals(4, zoomLevels.size());
        assertEquals(4, (int) zoomLevels.get(0));
        assertEquals(3, (int) zoomLevels.get(1));
        assertEquals(2, (int) zoomLevels.get(2));
        assertEquals(1, (int) zoomLevels.get(3));

    }

    @Test
    public void testEquals() throws Exception {

        final ZoomLevels zoomLevels1 = new ZoomLevels(4,2,3,0);
        final ZoomLevels zoomLevels2 = new ZoomLevels(4,2,3,1);
        final ZoomLevels zoomLevels3 = new ZoomLevels(1, 2, 3, 3, 3, 4);

        assertEquals(zoomLevels2, zoomLevels3);
        assertEquals(zoomLevels3, zoomLevels2);
        assertEquals(zoomLevels2, zoomLevels2);
        assertEquals(zoomLevels3, zoomLevels3);
        assertEquals(zoomLevels1, zoomLevels1);
        assertFalse(zoomLevels1.equals(zoomLevels2));
        assertFalse(zoomLevels1.equals(zoomLevels3));
        assertFalse(zoomLevels2.equals(zoomLevels1));

    }
}
