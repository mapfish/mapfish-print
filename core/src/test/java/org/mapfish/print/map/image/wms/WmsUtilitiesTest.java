package org.mapfish.print.map.image.wms;

import com.google.common.collect.Multimap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.URIUtils;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.awt.Dimension;
import java.net.URI;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WmsUtilitiesTest {
    @Test
    public void testMakeWmsGetLayerRequest() throws Exception {
        WmsLayerParam wmsLayerParams = new WmsLayerParam();
        wmsLayerParams.layers = new String[] {"layer1", "layer2", "layer3", "layer4", "layer5"};
        wmsLayerParams.imageFormat = "image/png";
        wmsLayerParams.customParams = new PJsonObject(new JSONObject("{\"map_resolution\":254}"), "customParams");
        wmsLayerParams.version = "1.1.1";
        URI commonURI = new URI("http://test.xyz/geoserver/wms?SERVICE=WMS");
        Dimension imageSize = new Dimension(200,300);
        ReferencedEnvelope env = new ReferencedEnvelope(0,10, 40, 50, CRS.decode("EPSG:4326"));
        final URI wmsURI = WmsUtilities.makeWmsGetLayerRequest(new TestHttpClientFactory(), wmsLayerParams, commonURI, imageSize, env);
        final Multimap<String, String> finalParams = URIUtils.getParameters(wmsURI);
        final Collection<String> layersParam = finalParams.get("LAYERS");
        assertNotNull(layersParam);
        assertEquals(1, layersParam.size());
        String[] resultlayers = layersParam.iterator().next().split(",");
        assertArrayEquals(wmsLayerParams.layers, resultlayers);
    }
}