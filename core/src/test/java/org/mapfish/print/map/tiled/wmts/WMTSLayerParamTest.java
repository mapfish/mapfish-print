package org.mapfish.print.map.tiled.wmts;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WMTSLayerParamTest {

    @Test
    public void testValidateBaseUrl() {
        WMTSLayerParam params = new WMTSLayerParam();

        params.requestEncoding = RequestEncoding.KVP;
        params.layer = "Layer";
        params.baseURL = "http://center_wmts_fixedscale_rest.com:1234/wmts";
        assertTrue(params.validateBaseUrl());

        params.requestEncoding = RequestEncoding.REST;
        params.matrixSet = "basemap";
        params.baseURL = "http://center_wmts_fixedscale_rest.com:1234/wmts";
        assertFalse(params.validateBaseUrl());

        params.requestEncoding = RequestEncoding.REST;
        params.matrixSet = "basemap";
        params.baseURL =
                "http://center_wmts_fixedscale_rest" +
                        ".com:1234/wmts/tiger-ny/{TileMatrixSet}/{TileMatrix}/{TileCol}/{TileRow}.tiff";
        assertTrue(params.validateBaseUrl());

        params.requestEncoding = RequestEncoding.REST;
        params.matrixSet = "basemap";
        params.style = "default";
        params.baseURL =
                "http://center_wmts_fixedscale_rest" +
                        ".com:1234/wmts/tiger-ny/{style}/{TileMatrixSet}/{TileMatrix}/{TileCol}/{TileRow}" +
                        ".tiff";
        assertTrue(params.validateBaseUrl());
    }

}
