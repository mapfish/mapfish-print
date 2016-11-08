package org.mapfish.print.attribute.map;

import org.junit.Test;
import org.mapfish.print.map.Scale;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.CLOSEST_HIGHER_SCALE_ON_TIE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.HIGHER_SCALE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.LOWER_SCALE;

public class ScaleAdjustmentAccuracyTest {

    private static final double TOLERANCE = 0.05;
    private static final ZoomLevels ZOOM_LEVELS = new ZoomLevels(20, 16, 12, 8, 4);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_8_RESULT = new ZoomLevelSnapStrategy.SearchResult(3, ZOOM_LEVELS);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_12_RESULT = new ZoomLevelSnapStrategy.SearchResult(2, ZOOM_LEVELS);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_16_RESULT = new ZoomLevelSnapStrategy.SearchResult(1, ZOOM_LEVELS);

    @Test
    public void testSearchCLOSEST_LOWER_SCALE_ON_MATCHMatch() throws Exception {
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(12, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(12.01, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(12.5, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(10, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(13, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(11.88, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(11, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(9, TOLERANCE, ZOOM_LEVELS));
    }

    @Test
    public void testSearchCLOSEST_HIGHER_SCALE_ON_MATCHMatch() throws Exception {
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(12, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(12.01, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(12.5, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(10, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(13, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(11.88, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(11, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(9, TOLERANCE, ZOOM_LEVELS));
    }

    @Test
    public void testSearchNextHighest() throws Exception {
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(12, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(12.01, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(12.5, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(10, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_16_RESULT, HIGHER_SCALE.search(13, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(11.88, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(11, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(9, TOLERANCE, ZOOM_LEVELS));
    }
    @Test
    public void testSearchLower() throws Exception {
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(12, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(12.01, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(12.5, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(10, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(13, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(11.88, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(11, TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(9, TOLERANCE, ZOOM_LEVELS));
    }
}
