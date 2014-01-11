package org.mapfish.print.map.readers;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.mapfish.print.FakeHttpd;
import org.mapfish.print.MapTestBasic;
import org.mapfish.print.Transformer;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;
import org.pvalsecc.misc.URIUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Test WMSMapReader
 * Created by Jesse on 1/10/14.
 */
public class WMSMapReaderTest extends MapTestBasic {
    private static final String CONTEXT_NAME = "/testServer";
    AtomicInteger port = new AtomicInteger(new Random().nextInt(55999)+5000);

    FakeHttpd server;

    @After
    public void tearDown() throws IOException, InterruptedException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testGetTileUri_Version1_1_1() throws Exception {
        Map<String, FakeHttpd.HttpAnswerer> routes = new HashMap<String, FakeHttpd.HttpAnswerer>();
        routes.put("/testServer", new FakeHttpd.HttpAnswerer(200, "OK",
                "application/xml", loadFileFromClasspath("/capabilities/wms1.1.1.xml")));

        final URI tileUri = createTileUri(loadSpec("1.1.1"), routes);

        final Map<String, List<String>> parameters = URIUtils.getParameters(tileUri.getRawQuery().toUpperCase());
        assertCommonParams(tileUri, parameters);
        assertEquals(""+tileUri, "1.1.1", parameters.get("VERSION").get(0));
        assertEquals(""+tileUri, "EPSG:4326", parameters.get("SRS").get(0));
        assertEquals(""+tileUri, "0.0,10.0,90.0,45.0", parameters.get("BBOX").get(0));
    }

    @Test
    public void testGetTileUri_VersionDefault() throws Exception {
        Map<String, FakeHttpd.HttpAnswerer> routes = new HashMap<String, FakeHttpd.HttpAnswerer>();
        routes.put("/testServer", new FakeHttpd.HttpAnswerer(200, "OK",
                "application/xml", loadFileFromClasspath("/capabilities/wms1.1.1.xml")));

        final URI tileUri = createTileUri(loadSpec(null), routes);

        final Map<String, List<String>> parameters = URIUtils.getParameters(tileUri.getRawQuery().toUpperCase());
        assertCommonParams(tileUri, parameters);
        assertEquals(""+tileUri, "1.1.1", parameters.get("VERSION").get(0));
        assertEquals(""+tileUri, "EPSG:4326", parameters.get("SRS").get(0));
        assertEquals(""+tileUri, "0.0,10.0,90.0,45.0", parameters.get("BBOX").get(0));
    }

    @Test
    public void testGetTileUri_Version1_3_0() throws Exception {
        Map<String, FakeHttpd.HttpAnswerer> routes = new HashMap<String, FakeHttpd.HttpAnswerer>();
        routes.put("/testServer", new FakeHttpd.HttpAnswerer(200, "OK",
                "application/xml", loadFileFromClasspath("/capabilities/wms1.3.0.xml")));
        final URI tileUri = createTileUri(loadSpec("1.3.0"), routes);

        final Map<String, List<String>> parameters = URIUtils.getParameters(tileUri.getRawQuery().toUpperCase());
        assertCommonParams(tileUri, parameters);
        assertEquals("" + tileUri, "1.3.0", parameters.get("VERSION").get(0));
        assertEquals(""+tileUri, "EPSG:4326", parameters.get("CRS").get(0));
        assertEquals(""+tileUri, "0.0,90.0,10.0,45.0", parameters.get("BBOX").get(0));
    }

    @Test
    public void testGetTileUri_VersionCustomParams_1_3_0() throws Exception {
        Map<String, FakeHttpd.HttpAnswerer> routes = new HashMap<String, FakeHttpd.HttpAnswerer>();
        routes.put("/testServer", new FakeHttpd.HttpAnswerer(200, "OK",
                "application/xml", loadFileFromClasspath("/capabilities/wms1.3.0.xml")));
        final PJsonObject jsonParams = loadSpec(null);
        JSONObject customParams = new JSONObject();
        customParams.accumulate("version", "1.3.0");
        jsonParams.getJSONArray("layers").getJSONObject(0).getInternalObj().accumulate("customParams", customParams);
        final URI tileUri = createTileUri(jsonParams, routes);

        final Map<String, List<String>> parameters = URIUtils.getParameters(tileUri.getRawQuery().toUpperCase());
        assertCommonParams(tileUri, parameters);
        assertEquals(""+tileUri, "1.3.0", parameters.get("VERSION").get(0));
        assertEquals(""+tileUri, "EPSG:4326", parameters.get("CRS").get(0));
        assertEquals(""+tileUri, "0.0,90.0,10.0,45.0", parameters.get("BBOX").get(0));
    }

    private PJsonObject loadSpec(String version) throws JSONException, IOException {
        String baseURL = "http://localhost:" + port.incrementAndGet() + CONTEXT_NAME;

        PJsonObject jsonParams = loadJson("layers/wms_layer_spec.json",
                new Replacement("@@baseURL@@", baseURL));
        if (version != null) {
            jsonParams.getJSONArray("layers").getJSONObject(0).getInternalObj().accumulate("version", version);
        }

        return jsonParams;
    }

    protected void assertCommonParams(URI tileUri, Map<String, List<String>> parameters) {
        assertEquals(""+tileUri, "WMS", parameters.get("SERVICE").get(0));
        assertEquals("" + tileUri, "GETMAP", parameters.get("REQUEST").get(0));
        assertEquals("" + tileUri, "LAYERNAME", parameters.get("LAYERS").get(0));
        assertEquals("" + tileUri, "WMSSTYLE", parameters.get("STYLES").get(0));
        assertEquals(""+tileUri, "DPI:300", parameters.get("FORMAT_OPTIONS").get(0));
        assertEquals(""+tileUri, "IMAGE/GIF", parameters.get("FORMAT").get(0));
    }

    private URI createTileUri(PJsonObject jsonParams, Map<String, FakeHttpd.HttpAnswerer> routes) throws IOException, JSONException, URISyntaxException {

        server = new FakeHttpd(port.get(), routes);
        server.start();

        final List<MapReader> mapReaders = new WMSMapReader.Factory().create("wms", context, jsonParams.getJSONArray("layers").getJSONObject(0));

        assertEquals(1, mapReaders.size());

        final WMSMapReader reader = (WMSMapReader) mapReaders.get(0);

        Transformer transformer = createTransformer();
        URI commonUri = reader.createCommonURI(transformer, "EPSG:4326", true);
        return reader.getTileUri(commonUri, transformer, 0, 10, 90, 45, 300, 300);
    }

    protected String loadFileFromClasspath(String classPathPath) throws IOException {
        final InputStream stream = MapTestBasic.class.getResourceAsStream(classPathPath);
        return FileUtilities.readWholeTextStream(stream, "UTF-8");
    }

    private Transformer createTransformer() {
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
        return new Transformer(centerX, centerY, paperWidth, paperHeight, scale, dpi, unitEnum,
                rotation, geodeticSRS, isIntegerSvg);
    }
}
