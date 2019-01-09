package org.mapfish.print.map.style.json;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MapfishJsonStyleVersion2Test {
    @Test
    public void testResolveAllValues() {
        Map<String, String> values = new HashMap<>();
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
