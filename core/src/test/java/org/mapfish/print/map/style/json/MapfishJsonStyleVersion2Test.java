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

package org.mapfish.print.map.style.json;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MapfishJsonStyleVersion2Test {
    @Test
    public void testResolveAllValues() throws Exception {
        Map<String, String> values = Maps.newHashMap();
        values.put("val1", "value");
        values.put("val2", "${val1}2");
        values.put("val3", "${val2}--3${val1}");
        values.put("val4", "pre- ${val3} -- ${val1} -- ${val2} -post");
        values.put("val5", "${doesNotExist}");

        Map<String, String> updated = MapfishJsonStyleVersion2.resolveAllValues(values);
        assertEquals(5, updated.size());
        assertEquals("value", updated.get("val1"));
        assertEquals("value2", updated.get("val2"));
        assertEquals("value2--3value", updated.get("val3"));
        assertEquals("pre- value2--3value -- value -- value2 -post", updated.get("val4"));
        assertEquals("${doesNotExist}", updated.get("val5"));
    }

}