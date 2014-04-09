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

package org.mapfish.print.map.tiled;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test param methods.
 * @author Jesse on 4/4/14.
 */
public class AbstractTiledLayerParamsTest {
    private static final String CUSTOM_PARAMS = "{\"key\": \"value\", \"key2\":[\"value1\", \"value2\"], \"key3\": null}";
    @Test
    public void testCustomParams() throws Exception {
        final AbstractTiledLayerParams params = new TestParams();

        assertEquals(0, params.getCustomParams().size());

        params.customParams = AbstractMapfishSpringTest.parseJSONObjectFromString(CUSTOM_PARAMS);

        final Multimap<String,String> paramMap = params.getCustomParams();

        assertCorrectParamsInMap(paramMap);
    }

    @Test
    public void testMergeableParams() throws Exception {
        final AbstractTiledLayerParams params = new TestParams();
        assertEquals(0, params.getMergeableParams().size());
        params.customParams = AbstractMapfishSpringTest.parseJSONObjectFromString(CUSTOM_PARAMS);

        final Multimap<String,String> paramMap = params.getCustomParams();

        assertCorrectParamsInMap(paramMap);
    }

    private void assertCorrectParamsInMap(Multimap<String, String> paramMap) {
        assertEquals(4, paramMap.values().size());
        assertEquals(3, paramMap.keySet().size());
        assertTrue(paramMap.containsEntry("key", "value"));
        assertTrue(paramMap.containsEntry("key2", "value1"));
        assertTrue(paramMap.containsEntry("key2", "value2"));
        assertTrue(paramMap.containsEntry("key3", "null"));
    }

    private static class TestParams extends AbstractTiledLayerParams {
        @Override
        public URI getBaseUri() {
            return null;
        }
    }
}
