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

package org.mapfish.print.servlet.oldapi;

import com.google.common.collect.Maps;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.servlet.MapPrinterServletTest;
import org.mapfish.print.servlet.NoSuchAppException;
import org.mapfish.print.servlet.ServletMapPrinterFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


@ContextConfiguration(locations = {
        MapPrinterServletTest.PRINT_CONTEXT,
})
public class OldAPIRequestConverterTest extends AbstractMapfishSpringTest {
    @Autowired
    private ServletMapPrinterFactory printerFactory;
        
    @Test
    public void testConvert() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        Configuration configuration = printerFactory.create("default").getConfiguration();
        JSONObject request = OldAPIRequestConverter.convert(loadRequestDataAsJson(), configuration).getInternalObj();
        
        assertNotNull(request);
        assertEquals("A4 Portrait", request.getString(Constants.JSON_LAYOUT_KEY));
        assertEquals("political-boundaries", request.getString(Constants.OUTPUT_FILENAME_KEY));
        assertEquals("pdf", request.getString("outputFormat"));
        
        assertTrue(request.has("attributes"));
        JSONObject attributes = request.getJSONObject("attributes");
        assertEquals("Map title", attributes.getString("title"));
        assertEquals("Comment on the map", attributes.getString("comment"));
        assertEquals(1, attributes.getInt("customParam1"));
        assertTrue(!attributes.has("units"));

        // map
        assertTrue(attributes.has("geojsonMap"));
        JSONObject map = attributes.getJSONObject("geojsonMap");
        assertEquals(5000.0, map.getDouble("scale"), 0.1);
        assertEquals(659307.58735556, map.getJSONArray("center").getDouble(0), 0.1);
        assertEquals(5711360.4205031, map.getJSONArray("center").getDouble(1), 0.1);
        assertEquals(0.0, map.getDouble("rotation"), 0.1);
        
        assertTrue(map.has("layers"));
        JSONArray layers = map.getJSONArray("layers");
        assertEquals(4, layers.length());
        
        JSONObject osmLayer = layers.getJSONObject(0);
        assertEquals("osm", osmLayer.getString("type"));
        assertEquals("http://tile.openstreetmap.org/", osmLayer.getString("baseURL"));
        assertEquals(1.0, osmLayer.getDouble("opacity"), 0.1);
        assertEquals("png", osmLayer.getString("imageFormat"));
        assertEquals(-20037508.34, osmLayer.getJSONArray("maxExtent").getDouble(0), 0.1);
        assertEquals(256, osmLayer.getJSONArray("tileSize").getInt(0));
        assertEquals(156543.03390625, osmLayer.getJSONArray("resolutions").getDouble(0), 0.1);
        
        JSONObject wmsLayer = layers.getJSONObject(1);
        assertEquals("wms", wmsLayer.getString("type"));
        assertEquals("http://demo.mapfish.org/mapfishsample/2.2/mapserv", wmsLayer.getString("baseURL"));
        assertEquals(1.0, wmsLayer.getDouble("opacity"), 0.1);
        assertEquals("image/png", wmsLayer.getString("imageFormat"));
        assertEquals("lines", wmsLayer.getJSONArray("styles").getString(0));
        assertEquals(true, wmsLayer.getJSONObject("customParams").getBoolean("TRANSPARENT"));
        assertEquals(false, wmsLayer.getJSONObject("customParams").has("version"));
        assertEquals("1.1.1", wmsLayer.getString("version"));
        
        JSONObject geojsonLayer1 = layers.getJSONObject(2);
        assertEquals("geojson", geojsonLayer1.getString("type"));
        JSONObject geoJson = geojsonLayer1.getJSONObject("geoJson");
        assertEquals(1, geoJson.getJSONArray("features").length());
        assertTrue(geojsonLayer1.has("style"));
        assertEquals("1", geojsonLayer1.getJSONObject("style").getString("version"));

        JSONObject geojsonLayer2 = layers.getJSONObject(3);
        assertEquals("geojson", geojsonLayer2.getString("type"));
        assertEquals("http://xyz.com/places.json", geojsonLayer2.getString("geoJson"));
        assertFalse(geojsonLayer2.has("style"));

        // table
        assertTrue(attributes.has("entries"));
        JSONObject table = attributes.getJSONObject("entries");
        assertTrue(table.has("columns"));
        assertTrue(table.has("data"));
        
        JSONArray columns = table.getJSONArray("columns");
        assertEquals(6, columns.length());
        assertEquals("ID", columns.getString(0));
        assertEquals("BFS-Nr.", columns.getString(1));
        
        JSONArray data = table.getJSONArray("data");
        assertEquals(2, data.length());
        assertEquals("27634972", data.getJSONArray(0).getString(0));
        assertEquals("27634973", data.getJSONArray(1).getString(0));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConvertInvalidTemplate() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        Configuration configuration = printerFactory.create("wrong-layout").getConfiguration();
        // will trigger an exception, because the configuration uses a 
        // different layout than specified in the request
        OldAPIRequestConverter.convert(loadRequestDataAsJson(), configuration);
    }

    private void setUpConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(OldAPIMapPrinterServletTest.class, "config-old-api.yaml").getAbsolutePath());
        configFiles.put("wrong-layout", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private PJsonObject loadRequestDataAsJson() throws IOException {
        return AbstractMapfishSpringTest.parseJSONObjectFromFile(OldAPIRequestConverterTest.class, "requestData-old-api-all.json");
    }
}
