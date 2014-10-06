/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.attribute.map;

import org.junit.Test;
import org.mapfish.print.map.Scale;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.CLOSEST_HIGHER_SCALE_ON_TIE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.HIGHER_SCALE;
import static org.mapfish.print.attribute.map.ZoomLevelSnapStrategy.LOWER_SCALE;

/**
 * @author Jesse on 4/1/14.
 */
public class ScaleAdjustmentAccuracyTest {

    private static final double TOLERANCE = 0.05;
    private static final ZoomLevels ZOOM_LEVELS = new ZoomLevels(20, 16, 12, 8, 4);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_8_RESULT = new ZoomLevelSnapStrategy.SearchResult(3, ZOOM_LEVELS);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_12_RESULT = new ZoomLevelSnapStrategy.SearchResult(2, ZOOM_LEVELS);
    private static final ZoomLevelSnapStrategy.SearchResult SCALE_16_RESULT = new ZoomLevelSnapStrategy.SearchResult(1, ZOOM_LEVELS);

    @Test
    public void testSearchCLOSEST_LOWER_SCALE_ON_MATCHMatch() throws Exception {
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(12), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(12.01), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(12.5), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(10), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(13), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(11.88), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(11), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_LOWER_SCALE_ON_TIE.search(new Scale(9), TOLERANCE, ZOOM_LEVELS));
    }

    @Test
    public void testSearchCLOSEST_HIGHER_SCALE_ON_MATCHMatch() throws Exception {
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(12), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(12.01), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(12.5), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(10), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(13), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(11.88), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(11), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, CLOSEST_HIGHER_SCALE_ON_TIE.search(new Scale(9), TOLERANCE, ZOOM_LEVELS));
    }

    @Test
    public void testSearchNextHighest() throws Exception {
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(new Scale(12), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(new Scale(12.01), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(new Scale(12.5), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(new Scale(10), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_16_RESULT, HIGHER_SCALE.search(new Scale(13), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(new Scale(11.88), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(new Scale(11), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, HIGHER_SCALE.search(new Scale(9), TOLERANCE, ZOOM_LEVELS));
    }
    @Test
    public void testSearchLower() throws Exception {
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(new Scale(12), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(new Scale(12.01), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(new Scale(12.5), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(new Scale(10), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(new Scale(13), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_12_RESULT, LOWER_SCALE.search(new Scale(11.88), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(new Scale(11), TOLERANCE, ZOOM_LEVELS));
        assertEquals(SCALE_8_RESULT, LOWER_SCALE.search(new Scale(9), TOLERANCE, ZOOM_LEVELS));
    }
}
