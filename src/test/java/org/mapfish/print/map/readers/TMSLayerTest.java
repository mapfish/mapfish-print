package org.mapfish.print.map.readers;

import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.*;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;

import java.io.File;
import java.util.*;

public class TMSLayerTest extends MapTestBasic {

    PJsonObject tmsSpec;
    TmsMapReader tmsreader;

    public TMSLayerTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();

        tmsSpec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File(TMSLayerTest.class.getClassLoader()
                .getResource("layers/layer_spec.json").getFile())));

    }

    protected void tearDown() throws Exception {

        super.tearDown();
    }

	public void testNoOrigin() throws JSONException{

        JSONObject tms_full = tmsSpec.getInternalObj();
        tms_full.accumulate("tileOrigin", null);
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not initiated by default as expected",0.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not initiated by default as expected",0.0f,tmsreader.tileCacheLayerInfo.originY);
    }

    public void testOriginXY() throws JSONException{
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("x",-10.0f);
        origin.put("y",-20.0f);

        tms_full.accumulate("tileOrigin", null);
        tms_full.accumulate("origin", new JSONObject(origin));
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via origin.x) as expected",-10.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not set (via origin.y) as expected",-20.0f,tmsreader.tileCacheLayerInfo.originY);
    }

    public void testTileOriginXY() throws JSONException{
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("x",-10.0f);
        origin.put("y",-20.0f);

        tms_full.accumulate("tileOrigin", new JSONObject(origin));
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via tileOrigin.x) as expected",-10.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not set (via tileOrigin.y) as expected",-20.0f,tmsreader.tileCacheLayerInfo.originY);
    }

    public void testOriginLatLon() throws JSONException{
        JSONObject tms_full = tmsSpec.getInternalObj();
        Map<String, Float> origin = new HashMap<String, Float>();
        origin.put("lat",-20.0f);
        origin.put("lon",-10.0f);

        tms_full.accumulate("tileOrigin", new JSONObject(origin));
        tms_full.accumulate("origin", null);
        tmsSpec = new PJsonObject(tms_full, "");

        tmsreader = new TmsMapReader("foo", context, tmsSpec);

        assertEquals("Origin X is not set (via tileOrigin.lon) as expected",-10.0f,tmsreader.tileCacheLayerInfo.originX);
        assertEquals("Origin Y is not set (via tileOrigin.lat) as expected",-20.0f,tmsreader.tileCacheLayerInfo.originY);

    }

}
