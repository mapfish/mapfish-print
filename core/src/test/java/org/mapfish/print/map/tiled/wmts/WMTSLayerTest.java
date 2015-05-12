package org.mapfish.print.map.tiled.wmts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jesse on 5/11/2015.
 */
public class WMTSLayerTest {

    @Test
    public void testCreateRestURI() throws Exception {
        WMTSLayerParam param = new WMTSLayerParam();
        param.layer = "wmts_layer";
        param.matrixSet = "matrix_set";
        param.baseURL = "http://test_server/mapproxy_4_v3/wmts/{Layer}/{TileMatrixSet}/{TileMatrix}/{TileCol}/{TileRow}.png";
        String restURI = WMTSLayer.createRestURI(param.baseURL, "the_matrix_id", 4, 5, param).toString();

        assertEquals("http://test_server/mapproxy_4_v3/wmts/wmts_layer/matrix_set/the_matrix_id/5/4.png", restURI);
    }
}