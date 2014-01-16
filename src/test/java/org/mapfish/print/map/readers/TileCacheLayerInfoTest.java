package org.mapfish.print.map.readers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Created by Jesse on 12/19/13.
 */
public class TileCacheLayerInfoTest {
    @Test
    public void testGetNearestResolution() throws Exception {
        String resolutions = "2800,1400,700,350,175,84,42,21,11.2,5.6,2.8,1.4,0.7,0.35,0.14,0.07";
        int width = 256;
        int height = 256;
        float minX = 0.0f;
        float minY = 0.0f;
        float maxX = 700000.0f;
        float maxY = 1300000.0f;
        String extension = "image/png";
        float originX = 0.0f;
        float originY = 0.0f;
        final TileCacheLayerInfo info = new TileCacheLayerInfo(resolutions, width, height, minX, minY, maxX, maxY,
                extension, originX, originY);


        for (double v : info.getResolutions()) {
            assertEquals(v, info.getNearestResolution(v).value, 0.000001);

            // test the common case where rounding gets in the way of calculating the perfect target resolution
            assertEquals(v, info.getNearestResolution(v - 0.00001f).value, 0.000001);
        }


        // Specific bug encountered
        assertEquals(11.2, info.getNearestResolution(11.19995f).value, 0.00001);

        final TileCacheLayerInfo.ResolutionInfo expected = new TileCacheLayerInfo.ResolutionInfo(5, 84);
        assertEquals(expected, info.getNearestResolution(84));
        assertEquals(expected, info.getNearestResolution(84.001f));
        assertEquals(expected, info.getNearestResolution(100));
        assertEquals(expected, info.getNearestResolution(83.001f));
        assertEquals(expected, info.getNearestResolution(84 * info.getResolutionTolerance()));
    }
}
