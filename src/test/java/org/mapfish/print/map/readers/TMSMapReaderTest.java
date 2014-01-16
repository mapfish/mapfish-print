package org.mapfish.print.map.readers;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapTestBasic;
import org.mapfish.print.Transformer;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TMSMapReaderTest extends MapTestBasic {

    PJsonObject tmsSpec;
    TmsMapReader tmsReader;


    @Before
    public void setUp() throws Exception {
        super.setUp();

        tmsSpec = loadJson("layers/tms_layer_spec.json");
    }

    @Test
    public void testNoOriginDefault0_0() throws JSONException {

        JSONObject tms_full = tmsSpec.getInternalObj();
        tms_full.accumulate("tileOrigin", null);
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        context.getConfig().setTmsDefaultOriginX(1f);
        context.getConfig().setTmsDefaultOriginY(2f);

        tmsReader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not initiated by default as expected", 1.0f, tmsReader.tileCacheLayerInfo.originX, 0.00001);
        assertEquals("Origin Y is not initiated by default as expected", 2.0f, tmsReader.tileCacheLayerInfo.originY, 0.00001);
    }

    @Test
    public void testNoOrigin_DefaultFromMaxExtent() throws JSONException {

        JSONObject tms_full = tmsSpec.getInternalObj();
        tms_full.accumulate("tileOrigin", null);
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        context.getConfig().setTmsDefaultOriginX(null);
        context.getConfig().setTmsDefaultOriginY(null);

        tmsReader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not initiated by default as expected", -20037508.342789f, tmsReader.tileCacheLayerInfo.originX, 0.00001);
        assertEquals("Origin Y is not initiated by default as expected", -20037508.342789f, tmsReader.tileCacheLayerInfo.originY, 0.00001);
    }

    @Test
    public void testOriginXY() throws JSONException {
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("x", -10.0f);
        origin.put("y", -20.0f);

        tms_full.accumulate("tileOrigin", null);
        tms_full.accumulate("origin", new JSONObject(origin));
        tmsSpec = new PJsonObject(tms_full, "");

        tmsReader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via origin.x) as expected", -10.0f, tmsReader.tileCacheLayerInfo.originX, 0.00001);
        assertEquals("Origin Y is not set (via origin.y) as expected", -20.0f, tmsReader.tileCacheLayerInfo.originY, 0.00001);
    }

    @Test
    public void testTileOriginXY() throws JSONException {
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("x", -10.0f);
        origin.put("y", -20.0f);

        tms_full.accumulate("tileOrigin", new JSONObject(origin));
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsReader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via tileOrigin.x) as expected", -10.0f, tmsReader.tileCacheLayerInfo.originX, 0.00001);
        assertEquals("Origin Y is not set (via tileOrigin.y) as expected", -20.0f, tmsReader.tileCacheLayerInfo.originY, 0.00001);
    }

    @Test
    public void testOriginLatLon() throws JSONException {
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("lat", -20.0f);
        origin.put("lon", -10.0f);

        tms_full.accumulate("tileOrigin", new JSONObject(origin));
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsReader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via tileOrigin.lon) as expected", -10.0f, tmsReader.tileCacheLayerInfo.originX, 0.00001);
        assertEquals("Origin Y is not set (via tileOrigin.lat) as expected", -20.0f, tmsReader.tileCacheLayerInfo.originY, 0.00001);

    }

    @Test
    public void testGetUri() throws Exception {
        String layerName = "layerName";
        tmsSpec = MapPrinter.parseSpec("{\n"
                                       + "        \"baseURL\":\"http://localhost/gs/gwc/service/tms/\",\n"
                                       + "        \"opacity\":0.7,\n"
                                       + "        \"singleTile\":false,\n"
                                       + "        \"type\":\"TMS\",\n"
                                       + "        \"layer\":\""+layerName+"\",\n"
                                       + "        \"maxExtent\":[0,0,700000,1300000],\n"
                                       + "        \"tileSize\":[256,256],\n"
                                       + "        \"resolutions\":[2800,1400,700,350,175,84,42,21,11.2,5.6,2.8,1.4,0.7,0.35,0.14,0.07],\n"
                                       + "        \"format\":\"png\"\n"
                                       + "    }");
        tmsReader = new TmsMapReader("foo", context, tmsSpec);
        URI commonUri = new URI("http://localhost/gs/");
        float centerX = 430552.3f;
        float centerY = 265431.9f;
        float paperWidth = 440.0f;
        float paperHeight = 483.0f;
        int scale = 75000;
        int dpi = 300;
        DistanceUnit unitEnum = DistanceUnit.fromString("m");
        double rotation = 0.0;
        String geodeticSRS = null;
        boolean isIntegerSvg = true;
        Transformer transformer = new Transformer(centerX, centerY, paperWidth, paperHeight, scale, dpi, unitEnum,
                rotation, geodeticSRS, isIntegerSvg, false);

        float minGeoX = 424345.6f;
        float minGeoY = 258048.0f;
        float maxGeoX = 427212.78f;
        float maxGeoY = 260915.2f;
        int w = 256;
        int h = 256;
        final URI tileUri1 = tmsReader.getTileUri(commonUri, transformer, minGeoX, minGeoY, maxGeoX, maxGeoY, w, h);
        assertEquals("/08/148/90.png", tileUri1.toString().split("layerName")[1]);

        minGeoX = 427212.78f;
        minGeoY = 258048.0f;
        maxGeoX = 430079.97f;
        maxGeoY = 260915.2f;
        w = 256;
        h = 256;
        final URI tileUri2 = tmsReader.getTileUri(commonUri, transformer, minGeoX, minGeoY, maxGeoX, maxGeoY, w, h);
        assertEquals("/08/149/90.png", tileUri2.toString().split("layerName")[1]);

        minGeoX = 430079.97f;
        minGeoY = 258048.0f;
        maxGeoX = 432947.16f;
        maxGeoY = 260915.2f;
        w = 256;
        h = 256;
        final URI tileUri3 = tmsReader.getTileUri(commonUri, transformer, minGeoX, minGeoY, maxGeoX, maxGeoY, w, h);
        assertEquals("/08/150/90.png", tileUri3.toString().split("layerName")[1]);

        minGeoX = 432947.16f;
        minGeoY = 258048.0f;
        maxGeoX = 435814.34f;
        maxGeoY = 260915.2f;
        w = 256;
        h = 256;
        final URI tileUri4 = tmsReader.getTileUri(commonUri, transformer, minGeoX, minGeoY, maxGeoX, maxGeoY, w, h);
        assertEquals("/08/151/90.png", tileUri4.toString().split("layerName")[1]);

        System.out.println(tileUri1);
        System.out.println(tileUri2);
        System.out.println(tileUri3);
        System.out.println(tileUri4);
    }
}
