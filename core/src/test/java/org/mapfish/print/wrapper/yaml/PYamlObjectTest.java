package org.mapfish.print.wrapper.yaml;

import org.junit.Test;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PYamlObjectTest {
    @Test
    public void testToJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("att1", 1);
        map.put("att2", new Object[]{1, 2});

        Map<String, Object> embedded = new HashMap<>();
        embedded.put("embeddedAtt1", true);
        Map<String, Object> embeddedEmbedded = new HashMap<>();
        embeddedEmbedded.put("ee1", 1);
        embedded.put("embeddedAtt2", embeddedEmbedded);
        embedded.put("embeddedAtt3", Arrays.asList("one", "two", "three"));
        map.put("att3", embedded);
        final PJsonObject test = new PYamlObject(map, "test").toJSON();

        assertEquals(3, test.size());
        assertEquals(1, test.getInt("att1"));

        PJsonArray array1 = test.getJSONArray("att2");
        assertEquals(2, array1.size());
        assertEquals(1, array1.get(0));
        assertEquals(2, array1.get(1));

        PJsonObject embeddedJson = test.getJSONObject("att3");
        assertEquals(3, embeddedJson.size());
        assertTrue(embeddedJson.has("embeddedAtt1"));

        PJsonObject embeddedEmbeddedJson = embeddedJson.getJSONObject("embeddedAtt2");
        assertEquals(1, embeddedEmbeddedJson.size());
        assertEquals(1, embeddedEmbeddedJson.getInt("ee1"));

        PJsonArray array2 = embeddedJson.getJSONArray("embeddedAtt3");
        assertEquals(3, array2.size());
        assertEquals("one", array2.getString(0));
        assertEquals("two", array2.getString(1));
        assertEquals("three", array2.getString(2));
    }
}
