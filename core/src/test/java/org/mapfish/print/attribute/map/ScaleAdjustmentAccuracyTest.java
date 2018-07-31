package org.mapfish.print.attribute.map;

import org.junit.Test;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.Constants.PDF_DPI;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.CLOSEST_HIGHER_SCALE_ON_TIE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.HIGHER_SCALE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.LOWER_SCALE;

public class ScaleAdjustmentAccuracyTest {

    private static final double TOLERANCE = 0.05;
    private static final ZoomLevels ZOOM_LEVELS = new ZoomLevels(20, 16, 12, 8, 4);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_8_RESULT =
            new ZoomLevelSnapStrategy.SearchResult(3, ZOOM_LEVELS);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_12_RESULT =
            new ZoomLevelSnapStrategy.SearchResult(2, ZOOM_LEVELS);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_16_RESULT =
            new ZoomLevelSnapStrategy.SearchResult(1, ZOOM_LEVELS);

    @Test
    public void testSearchCLOSEST_LOWER_SCALE_ON_MATCHMatch() {
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(12, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(12.01, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(12.5, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(10, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(13, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(11.88, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(11, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(
                new Scale(9, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
    }

    @Test
    public void testSearchCLOSEST_HIGHER_SCALE_ON_MATCHMatch() {
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(12, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(12.01, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(12.5, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(10.00001, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(13, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(11.88, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(11, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(
                new Scale(9, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
    }

    @Test
    public void testSearchNextHighest() {
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(
                new Scale(12, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(
                new Scale(12.01, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(
                new Scale(12.5, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(
                new Scale(10, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_16_RESULT, HIGHER_SCALE.search(
                new Scale(13, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(
                new Scale(11.88, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(
                new Scale(11, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(
                new Scale(9, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
    }

    @Test
    public void testSearchLower() {
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(
                new Scale(12, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(
                new Scale(12.01, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(
                new Scale(12.5, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(
                new Scale(10, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(
                new Scale(13, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(
                new Scale(11.88, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(
                new Scale(11, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(
                new Scale(9, DistanceUnit.M, PDF_DPI), TOLERANCE, ZOOM_LEVELS));
    }
}
