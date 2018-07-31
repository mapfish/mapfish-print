package org.mapfish.print.map.tiled;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test param methods.
 */
public class AbstractWMXLayerParamsTest {
    private static final String CUSTOM_PARAMS =
            "{\"key\": \"value\", \"key2\":[\"value1\", \"value2\"], \"key3\": null}";

    @Test
    public void testCustomParams() {
        final AbstractWMXLayerParams params = new TestParams();

        assertEquals(0, params.getCustomParams().size());

        params.customParams = AbstractMapfishSpringTest.parseJSONObjectFromString(CUSTOM_PARAMS);

        final Multimap<String, String> paramMap = params.getCustomParams();

        assertCorrectParamsInMap(paramMap);
    }

    @Test
    public void testMergeableParams() {
        final AbstractWMXLayerParams params = new TestParams();
        assertEquals(0, params.getMergeableParams().size());
        params.customParams = AbstractMapfishSpringTest.parseJSONObjectFromString(CUSTOM_PARAMS);

        final Multimap<String, String> paramMap = params.getCustomParams();

        assertCorrectParamsInMap(paramMap);
    }

    private void assertCorrectParamsInMap(Multimap<String, String> paramMap) {
        assertEquals(4, paramMap.values().size());
        assertEquals(3, paramMap.keySet().size());
        assertTrue(paramMap.containsEntry("key", "value"));
        assertTrue(paramMap.containsEntry("key2", "value1"));
        assertTrue(paramMap.containsEntry("key2", "value2"));
        assertTrue(paramMap.containsEntry("key3", ""));
    }

    private static class TestParams extends AbstractWMXLayerParams {
        @Override
        public String getBaseUrl() {
            return null;
        }
    }
}
