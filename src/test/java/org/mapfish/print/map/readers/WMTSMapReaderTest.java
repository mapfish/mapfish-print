package org.mapfish.print.map.readers;

import com.google.common.io.ByteStreams;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.FakeHttpd;
import org.mapfish.print.MapTestBasic;
import org.mapfish.print.Transformer;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.URIUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mapfish.print.map.readers.WMTSServiceInfoTest.CAPABILITIES_WMTS1_0_0_XML;

/**
 * Basic testing of reader.
 * <p/>
 * Created by Jesse on 12/19/13.
 */
public class WMTSMapReaderTest extends MapTestBasic {

    private PJsonObject wmtsSpec;
    FakeHttpd server;

    @After
    public void tearDown() throws IOException, InterruptedException {
        if (server != null) {
            server.shutdown();
        }
    }
    @Before
    public void setUp() throws Exception {
        super.setUp();

        wmtsSpec = loadJson("layers/wmts_layer_spec.json");

    }

    @Test
    public void testRequireMatrixIdsOrTileOrigin() throws Exception {
        try {
            wmtsSpec.getInternalObj().accumulate("zoomOffset", 1);
            new WMTSMapReader.Factory().create("layer", context, wmtsSpec);
            fail("An exception should be raised");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("matrixIds") && e.getMessage().contains("tileOrigin"));
        }
    }

    @Test
    public void testRequireMatrixIdsOrZoomOffset() throws Exception {
        try {
            wmtsSpec.getInternalObj().accumulate("tileOrigin", new JSONArray(new Integer[]{0, 0}));
            new WMTSMapReader.Factory().create("layer", context, wmtsSpec);
            fail("An exception should be raised");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("matrixIds") && e.getMessage().contains("zoomOffset"));
        }
    }

    @Test
    public void testRequireMatrixIdsOrFormatSuffix() throws Exception {
        try {
            wmtsSpec.getInternalObj().accumulate("tileOrigin", new JSONArray(new Integer[]{0, 0}));
            wmtsSpec.getInternalObj().accumulate("zoomOffset", 1);
            new WMTSMapReader.Factory().create("layer", context, wmtsSpec);
            fail("An exception should be raised");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("matrixIds") && e.getMessage().contains("formatSuffix") && e.getMessage().contains
                    ("extension"));
        }
    }

    @Test
    public void testValidConfig() throws Exception {
        wmtsSpec.getInternalObj().accumulate("tileOrigin", new JSONArray(new Integer[]{0, 0}));
        wmtsSpec.getInternalObj().accumulate("zoomOffset", 1);
        wmtsSpec.getInternalObj().accumulate("extension", "png");
        WMTSMapReader reader = (WMTSMapReader) new WMTSMapReader.Factory().create("layer", context, wmtsSpec).get(0);
        assertEquals("png", reader.tileCacheLayerInfo.getExtension());
    }

    @Test
    public void testGetTileUriUsingCapabilities() throws Exception {
        byte[] capabilities = ByteStreams.toByteArray(WMTSMapReader.class.getResourceAsStream(CAPABILITIES_WMTS1_0_0_XML));
        final String path = "/e2egeoserver/gwc/service/wmts";
        server = new FakeHttpd(FakeHttpd.Route.xmlResponse(path, capabilities));
        server.start();

        wmtsSpec = loadJson("layers/wmts1_0_0-full-kvp.json",
                new Replacement("@@host@@", "localhost"), new Replacement("@@port@@", server.getPort()));

        WMTSMapReader reader = (WMTSMapReader) new WMTSMapReader.Factory().create("layer", context, wmtsSpec.getJSONArray("layers").getJSONObject(0)).get(0);

        URI commonUri = new URI("http://localhost:"+server.getPort()+path);
        Transformer transformer = createTransformer();
        double minGeoX = -8267428.5;
        double minGeoY = 4960457.0;
        double maxGeoX = -8257644.560380859;
        double maxGeoY = 4970240.939619141;
        int w = 256;
        int h = 256;
        final URI tileUri = reader.getTileUri(commonUri, transformer, minGeoX, minGeoY, maxGeoX, maxGeoY, w, h);
        final Map<String,List<String>> parameters = URIUtils.getParameters(tileUri);
        assertSingle(parameters, "SERVICE", "WMTS");
        assertSingle(parameters, "REQUEST", "GetTile");
        assertSingle(parameters, "VERSION", "1.0.0");
        assertSingle(parameters, "LAYER", "tiger-ny");
        assertSingle(parameters, "STYLE", "");
        assertSingle(parameters, "TILEMATRIXSET", "EPSG:900913");
        assertSingle(parameters, "TILEMATRIX", "EPSG:900913:12");
        assertSingle(parameters, "TILEROW", "1540");
        assertSingle(parameters, "TILECOL", "1203");
        assertSingle(parameters, "FORMAT", "image/png");
    }
    @Test
    public void testGetTileUriNoCapabilities() throws Exception {
        final String path = "/e2egeoserver/gwc/service/wmts";
        server = new FakeHttpd();
        server.start();

        wmtsSpec = loadJson("layers/wmts1_0_0-full-kvp.json",
                new Replacement("@@host@@", "localhost"), new Replacement("@@port@@", server.getPort()));

        WMTSMapReader reader = (WMTSMapReader) new WMTSMapReader.Factory().create("layer", context, wmtsSpec.getJSONArray("layers").getJSONObject(0)).get(0);

        URI commonUri = new URI("http://localhost:"+server.getPort()+path);
        Transformer transformer = createTransformer();
        double minGeoX = -8267428.5;
        double minGeoY = 4960457.0;
        double maxGeoX = -8257644.560380859;
        double maxGeoY = 4970240.939619141;
        int w = 256;
        int h = 256;
        final URI tileUri = reader.getTileUri(commonUri, transformer, minGeoX, minGeoY, maxGeoX, maxGeoY, w, h);
        final Map<String,List<String>> parameters = URIUtils.getParameters(tileUri);
        assertSingle(parameters, "SERVICE", "WMTS");
        assertSingle(parameters, "REQUEST", "GetTile");
        assertSingle(parameters, "VERSION", "1.0.0");
        assertSingle(parameters, "LAYER", "tiger-ny");
        assertSingle(parameters, "STYLE", "");
        assertSingle(parameters, "TILEMATRIXSET", "EPSG:900913");
        assertSingle(parameters, "TILEMATRIX", "12");
        assertSingle(parameters, "TILEROW", "1540");
        assertSingle(parameters, "TILECOL", "1203");
        assertSingle(parameters, "FORMAT", "image/png");
    }

    private void assertSingle(Map<String, List<String>> parameters, String parameter, String value) {
        List<String> found = parameters.get(parameter);
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(value, found.get(0));
    }


    private Transformer createTransformer() {
        double centerX = -8236566.427097;
        double centerY = 4976131.070529;
        float paperWidth = 780.0f;
        float paperHeight = 330.0f;
        double scale = 200000.0;
        int dpi = 90;
        DistanceUnit unitEnum = DistanceUnit.M;
        double rotation = 0.0;
        String geodeticSRS = null;
        boolean isIntegerSvg = true;
        return new Transformer(centerX, centerY, paperWidth, paperHeight, scale, dpi, unitEnum,
                rotation, geodeticSRS, isIntegerSvg, false);
    }
}
