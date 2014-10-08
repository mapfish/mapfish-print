package org.mapfish.print.map.tiled.osm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OsmLayerParamTest {

    @Test
    public void testValidateBaseUrl() {
        OsmLayerParam params = new OsmLayerParam();

        params.baseURL = "http://tile.openstreetmap.org";
        assertTrue(params.validateBaseUrl());

        params.baseURL = "http://tile.openstreetmap.org/";
        assertTrue(params.validateBaseUrl());

        params.baseURL = "http://tile.openstreetmap.org/{z}/{x}/{y}.png";
        assertTrue(params.validateBaseUrl());

        params.baseURL = "http://www.maptiler.org/example-usgs-drg-grand-canyon-gtiff/{z}/{x}/{-y}.png";
        assertTrue(params.validateBaseUrl());

        params.baseURL = "http://tile.openstreetmap.org/{z}/{x}/{Y}.png";
        assertFalse(params.validateBaseUrl());

        params.baseURL = "http://tile.openstreetmap.org/{z}/{x}/{Y.png";
        assertFalse(params.validateBaseUrl());

        params.baseURL = "http://tile.openstreetmap.org/{a}/{x}/{y}.png";
        assertFalse(params.validateBaseUrl());
    }

}
