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
import org.mapfish.print.wrapper.json.PJsonArray;
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
        MapPrinterServletTest.PRINT_CONTEXT
})
public class OldAPIRequestConverterTest extends AbstractMapfishSpringTest {
    @Autowired
    private ServletMapPrinterFactory printerFactory;

    @Test
    public void testConvert() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        Configuration configuration = printerFactory.create("default").getConfiguration();
        JSONObject request = OldAPIRequestConverter.convert(loadRequestDataAsJson("requestData-old-api-all.json"), configuration).getInternalObj();

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
        assertTrue(attributes.has("map"));
        JSONObject map = attributes.getJSONObject("map");
        assertEquals(5000.0, map.getDouble("scale"), 0.1);
        assertEquals(659307.58735556, map.getJSONArray("center").getDouble(0), 0.1);
        assertEquals(5711360.4205031, map.getJSONArray("center").getDouble(1), 0.1);
        assertEquals(-45.0, map.getDouble("rotation"), 0.1);

        assertTrue(map.has("layers"));
        JSONArray layers = map.getJSONArray("layers");
        assertEquals(5, layers.length());

        JSONObject wmtsLayer = layers.getJSONObject(0);
        assertEquals("wmts", wmtsLayer.getString("type"));
        assertEquals("http://center_wmts_fixedscale.com:1234/wmts", wmtsLayer.getString("baseURL"));
        assertEquals(0.5, wmtsLayer.getDouble("opacity"), 0.1);
        assertEquals("image/tiff", wmtsLayer.getString("imageFormat"));
        assertEquals("EPSG:900913", wmtsLayer.getString("matrixSet"));
        assertEquals("normal", wmtsLayer.getString("style"));
        JSONArray matrices =  wmtsLayer.getJSONArray("matrices");
        assertEquals(6,matrices.length());
        JSONObject matrix = matrices.getJSONObject(0);
        assertEquals("EPSG:900913:12",matrix.getString("identifier"));
        assertEquals(136494.69334738597,matrix.getDouble("scaleDenominator"),0.00001);
        assertEquals(256, matrix.getJSONArray("tileSize").getInt(0));
        assertEquals(4096, matrix.getJSONArray("matrixSize").getInt(0));
        assertEquals(-2.003750834E7, matrix.getJSONArray("topLeftCorner").getDouble(0),0.00001);



        JSONObject osmLayer = layers.getJSONObject(1);
        assertEquals("osm", osmLayer.getString("type"));
        assertEquals("http://tile.openstreetmap.org/", osmLayer.getString("baseURL"));
        assertEquals(1.0, osmLayer.getDouble("opacity"), 0.1);
        assertEquals("png", osmLayer.getString("imageFormat"));
        assertEquals(-20037508.34, osmLayer.getJSONArray("maxExtent").getDouble(0), 0.1);
        assertEquals(256, osmLayer.getJSONArray("tileSize").getInt(0));
        assertEquals(156543.03390625, osmLayer.getJSONArray("resolutions").getDouble(0), 0.1);

        JSONObject wmsLayer = layers.getJSONObject(2);
        assertEquals("wms", wmsLayer.getString("type"));
        assertEquals("http://demo.mapfish.org/mapfishsample/2.2/mapserv", wmsLayer.getString("baseURL"));
        assertEquals(1.0, wmsLayer.getDouble("opacity"), 0.1);
        assertEquals("image/png", wmsLayer.getString("imageFormat"));
        assertEquals("lines", wmsLayer.getJSONArray("styles").getString(0));
        assertEquals(true, wmsLayer.getJSONObject("customParams").getBoolean("TRANSPARENT"));
        assertEquals(false, wmsLayer.getJSONObject("customParams").has("version"));
        assertEquals("1.1.1", wmsLayer.getString("version"));

        JSONObject geojsonLayer1 = layers.getJSONObject(3);
        assertEquals("geojson", geojsonLayer1.getString("type"));
        JSONObject geoJson = geojsonLayer1.getJSONObject("geoJson");
        assertEquals(1, geoJson.getJSONArray("features").length());
        assertTrue(geojsonLayer1.has("style"));
        assertEquals("1", geojsonLayer1.getJSONObject("style").getString("version"));

        JSONObject geojsonLayer2 = layers.getJSONObject(4);
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


        // legend
        assertLegend(attributes, "legend1", "legend1");
        assertLegend(attributes, "legend2", "legend2", "legend3");
    }

    private void assertLegend(JSONObject attributes, String legendAttName, String... rows) throws JSONException {
        assertTrue(attributes.has(legendAttName));
        final JSONObject legendJson = attributes.getJSONObject(legendAttName);
        if (rows.length == 1) {
            assertLegend(legendJson, rows[0]);
        } else {
            final JSONArray classes = legendJson.getJSONArray("classes");
            for (int i = 0; i < rows.length; i++) {
                assertLegend(classes.getJSONObject(i), rows[i]);
            }
        }
    }

    private void assertLegend(JSONObject legendJson, String legendAttName) throws JSONException {
        assertTrue(legendJson.has("name"));
        assertEquals(legendAttName, legendJson.getString("name"));

        assertTrue(legendJson.has("classes"));

        JSONArray classes = legendJson.getJSONArray("classes");
        assertEquals(1, classes.length());
        final JSONObject firstClass = classes.getJSONObject(0);
        assertEquals(2, firstClass.length());
        assertEquals(legendAttName, firstClass.getString("name"));
        JSONArray icons = firstClass.getJSONArray("icons");
        assertEquals(1, icons.length());
        assertEquals("file://legend-ico.png", icons.getString(0));
    }

    @Test
    public void testConvertTableInConfigNotInRequest() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        Configuration configuration = printerFactory.create("default").getConfiguration();
        final PJsonObject v2ApiRequest = loadRequestDataAsJson("requestData-old-api-no-table-data.json");
        JSONObject request = OldAPIRequestConverter.convert(v2ApiRequest, configuration).getInternalObj();

        assertTrue(request.has("attributes"));
        JSONObject attributes = request.getJSONObject("attributes");


        assertTrue(attributes.has("legend1"));
        assertTrue(attributes.has("legend2"));
        // table
        assertTrue(attributes.has("entries"));
        JSONObject table = attributes.getJSONObject("entries");
        assertTrue(table.has("columns"));
        assertTrue(table.has("data"));

        JSONArray columns = table.getJSONArray("columns");
        assertEquals(0, columns.length());

        JSONArray data = table.getJSONArray("data");
        assertEquals(0, data.length());
    }

    @Test
    public void testWmsLayer() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        PJsonObject oldApiJSON = parseJSONObjectFromFile(OldAPIRequestConverterTest.class, "wms-layer-order.json");
        Configuration configuration = printerFactory.create("default").getConfiguration();
        PJsonObject jsonObject = OldAPIRequestConverter.convert(oldApiJSON, configuration);

        PJsonArray layers = jsonObject.getJSONObject("attributes").getJSONObject("map").getJSONArray("layers");
        assertEquals(2, layers.size());
        PJsonArray wmsLayer1 = layers.getJSONObject(0).getJSONArray("layers");
        assertEquals(2, wmsLayer1.size());
        assertEquals("tiger:tiger_roads", wmsLayer1.getString(0));
        assertEquals("tiger:poi", wmsLayer1.getString(1));

        PJsonArray wmsLayer2 = layers.getJSONObject(1).getJSONArray("layers");
        assertEquals(2, wmsLayer2.size());
        assertEquals("nurc:Img_Sample", wmsLayer2.getString(0));
        assertEquals("tiger:poly_landmarks", wmsLayer2.getString(1));
    }

    @Test
    public void testTiledWmsLayer() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        PJsonObject oldApiJSON = parseJSONObjectFromFile(OldAPIRequestConverterTest.class, "wms-tiled.json");
        Configuration configuration = printerFactory.create("default").getConfiguration();
        PJsonObject jsonObject = OldAPIRequestConverter.convert(oldApiJSON, configuration);

        PJsonArray layers = jsonObject.getJSONObject("attributes").getJSONObject("map").getJSONArray("layers");
        assertEquals(1, layers.size());
        PJsonObject wmsLayer = layers.getJSONObject(0);
        assertEquals("tiledwms", wmsLayer.getString("type"));
        assertEquals("http://localhost:9876/e2egeoserver/wms", wmsLayer.getString("baseURL"));
        assertEquals(1.0, wmsLayer.getDouble("opacity"), 0.1);
        assertEquals("image/png", wmsLayer.getString("imageFormat"));
        assertEquals(2,wmsLayer.getJSONArray("tileSize").size());
        assertEquals(256,wmsLayer.getJSONArray("tileSize").getInt(0));

    }

    @Test
    public void testReverseLayerOrder() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        Configuration configuration = printerFactory.create("reverseLayers").getConfiguration();
        final PJsonObject v2ApiRequest = loadRequestDataAsJson("requestData-old-api-all.json");
        JSONObject request = OldAPIRequestConverter.convert(v2ApiRequest, configuration).getInternalObj();

        assertTrue(request.has("attributes"));
        JSONObject attributes = request.getJSONObject("attributes");

        JSONArray layers = attributes.getJSONObject("map").getJSONArray("layers");

        assertEquals("geojson", layers.getJSONObject(0).getString("type"));
        assertEquals("geojson", layers.getJSONObject(1).getString("type"));
        assertEquals("wms", layers.getJSONObject(2).getString("type"));
        assertEquals("osm", layers.getJSONObject(3).getString("type"));
        assertEquals("wmts", layers.getJSONObject(4).getString("type"));
        assertEquals(5, layers.length());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertInvalidTemplate() throws IOException, JSONException, NoSuchAppException, URISyntaxException {
        setUpConfigFiles();
        Configuration configuration = printerFactory.create("wrong-layout").getConfiguration();
        // will trigger an exception, because the configuration uses a
        // different layout than specified in the request
        OldAPIRequestConverter.convert(loadRequestDataAsJson("requestData-old-api-all.json"), configuration);
    }

    private void setUpConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(OldAPIMapPrinterServletTest.class, "config-old-api.yaml").getAbsolutePath());
        configFiles.put("reverseLayers", getFile(OldAPIMapPrinterServletTest.class, "config-old-api-reverse.yaml").getAbsolutePath());
        configFiles.put("wrong-layout", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private PJsonObject loadRequestDataAsJson(String fileName) throws IOException {
        return AbstractMapfishSpringTest.parseJSONObjectFromFile(OldAPIRequestConverterTest.class, fileName);
    }
}
