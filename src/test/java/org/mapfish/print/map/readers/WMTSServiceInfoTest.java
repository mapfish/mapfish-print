package org.mapfish.print.map.readers;

import com.google.common.io.ByteStreams;
import com.vividsolutions.jts.geom.Envelope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.FakeHttpd;
import org.mapfish.print.MapTestBasic;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test WMTSServiceInfo class.
 * <p/>
 * Created by Jesse on 1/20/14.
 */
public class WMTSServiceInfoTest extends MapTestBasic {

    public static final String CAPABILITIES_WMTS1_0_0_XML = "/capabilities/wmts1.0.0.xml";

    FakeHttpd server;
    byte[] capabilitiesDocument;
    String path = "/capabilities";
    private URI url;

    @Before
    public void before() throws IOException, URISyntaxException {
        capabilitiesDocument = ByteStreams.toByteArray(WMTSServiceInfo.class.getResourceAsStream(CAPABILITIES_WMTS1_0_0_XML));

        server = new FakeHttpd(FakeHttpd.Route.xmlResponse(path, capabilitiesDocument));
        this.url = new URI("http://localhost:" + server.getPort() + path);

        server.start();
    }

    @After
    public void after() throws IOException, InterruptedException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testReadCapabilities1_0_0() throws Exception {
        final WMTSServiceInfo info = WMTSServiceInfo.getInfo(url, context);

        assertEquals(21, info.tileCacheLayers.size());
        assertSame(info, WMTSServiceInfo.getInfo(url, context));
        final String layerId = "nurc:Img_Sample";
        assertSame(info.tileCacheLayers.get(layerId), WMTSServiceInfo.getLayerInfo(url, layerId, context));
        WmtsCapabilitiesInfo layer = info.tileCacheLayers.get(layerId);
        assertEquals(new Envelope(-130.85168, -62.0054, 20.7052, 54.1141), layer.getBounds());
        assertEquals("North America sample imagery", layer.getTitle());
        assertEquals("nurc:Img_Sample", layer.getIdentifier());
        assertEquals(2, layer.getFormats().size());
        assertTrue(layer.getFormats().contains("image/png"));
        assertTrue(layer.getFormats().contains("image/jpeg"));
        final Map<String, WMTSServiceInfo.TileMatrixSet> tileMatrices = layer.getTileMatrices();
        assertEquals(2, tileMatrices.size());
        assertTrue(tileMatrices.containsKey("EPSG:4326"));
        assertTrue(tileMatrices.containsKey("EPSG:900913"));
        final WMTSServiceInfo.TileMatrixSet epsg900913Matrices = tileMatrices.get("EPSG:900913");
        final WMTSServiceInfo.TileMatrixSet epsg4326Matrices = tileMatrices.get("EPSG:4326");
        assertEquals(22, epsg4326Matrices.limits.size());
        for (int i = 0; i < 22; i++) {
            assertTrue(epsg4326Matrices.limits.containsKey("EPSG:4326:" + i));
        }
        assertEquals(31, epsg900913Matrices.limits.size());
        for (int i = 0; i < 31; i++) {
            assertTrue("Expected EPSG:900913 limits to contain: EPSG:900913:" + i + " Limits:\n\t" + epsg900913Matrices,
                    epsg900913Matrices.limits.containsKey("EPSG:900913:" + i));
        }

    }
}
