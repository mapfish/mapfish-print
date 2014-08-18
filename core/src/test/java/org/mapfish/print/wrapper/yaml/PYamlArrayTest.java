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

package org.mapfish.print.wrapper.yaml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.wrapper.json.PJsonArray;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PYamlArrayTest {

    @Test
    public void testToJSON() throws Exception {
        Map<String, Object> embedded = Maps.newHashMap();
        embedded.put("a", 1);
        List<Object> array = Lists.newArrayList(1, embedded,
                Lists.newArrayList(1,2,3),
                new String[]{"a", "b", "c"});
        final PJsonArray test = new PYamlArray(null, array, "test").toJSON();
        assertEquals(4, test.size());

        assertEquals(1, test.get(0));

        final JSONObject embeddedJson = (JSONObject) test.get(1);
        assertEquals(1, embeddedJson.length());
        assertEquals(1, embeddedJson.getInt("a"));

        final JSONArray array1 = (JSONArray) test.get(2);
        assertEquals(3, array1.length());
        assertEquals(1, array1.getInt(0));
        assertEquals(2, array1.getInt(1));
        assertEquals(3, array1.getInt(2));

        final JSONArray array2 = (JSONArray) test.get(3);
        assertEquals(3, array2.length());
        assertEquals("a", array2.getString(0));
        assertEquals("b", array2.getString(1));
        assertEquals("c", array2.getString(2));

        assertTrue(test.getArray(3) instanceof PJsonArray);
        assertTrue(test.getJSONArray(3) != null);
    }
}