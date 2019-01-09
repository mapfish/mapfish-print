package org.mapfish.print.wrapper.yaml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.wrapper.json.PJsonArray;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PYamlArrayTest {

    @Test
    public void testToJSON() {
        Map<String, Object> embedded = new HashMap<>();
        embedded.put("a", 1);
        List<Object> array = Arrays.asList(1, embedded,
                                           Arrays.asList(1, 2, 3),
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
        assertNotNull(test.getJSONArray(3));
    }
}
