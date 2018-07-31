package org.mapfish.print.map.image.wms;

import com.google.common.collect.Multimap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.URIUtils;
import org.mapfish.print.map.image.wms.WmsLayerParam.ServerType;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.awt.Dimension;
import java.net.URI;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class WmsUtilitiesTest {

    @Test
    public void testMakeWmsGetLayerRequest() throws Exception {
        WmsLayerParam wmsLayerParams = new WmsLayerParam();
        wmsLayerParams.layers = new String[]{"layer1", "layer2", "layer3", "layer4", "layer5"};
        wmsLayerParams.imageFormat = "image/png";
        wmsLayerParams.customParams =
                new PJsonObject(new JSONObject("{\"map_resolution\":254}"), "customParams");
        wmsLayerParams.version = "1.1.1";
        URI commonURI = new URI("http://test.xyz/geoserver/wms?SERVICE=WMS");
        Dimension imageSize = new Dimension(200, 300);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 10, 40, 50, CRS.decode("EPSG:4326"));
        final URI wmsURI = WmsUtilities.makeWmsGetLayerRequest(
                wmsLayerParams, commonURI, imageSize, 72.0, 0.0, env);
        final Multimap<String, String> finalParams = URIUtils.getParameters(wmsURI);
        final Collection<String> layersParam = finalParams.get("LAYERS");
        assertNotNull(layersParam);
        assertEquals(1, layersParam.size());
        String[] resultlayers = layersParam.iterator().next().split(",");
        assertArrayEquals(wmsLayerParams.layers, resultlayers);
    }

    @Test
    public void testMakeWmsGetLayerRequestDpiMapServer() throws Exception {
        WmsLayerParam wmsLayerParams = new WmsLayerParam();
        wmsLayerParams.layers = new String[]{"layer1", "layer2", "layer3", "layer4", "layer5"};
        wmsLayerParams.imageFormat = "image/png";
        wmsLayerParams.version = "1.1.1";
        wmsLayerParams.serverType = ServerType.MAPSERVER;
        URI commonURI = new URI("http://test.xyz/geoserver/wms?SERVICE=WMS");
        Dimension imageSize = new Dimension(200, 300);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 10, 40, 50, CRS.decode("EPSG:4326"));
        final URI wmsURI = WmsUtilities.makeWmsGetLayerRequest(
                wmsLayerParams, commonURI, imageSize, 300.0, 0.0, env);
        final Multimap<String, String> finalParams = URIUtils.getParameters(wmsURI);
        final String mapResolution = finalParams.get("MAP_RESOLUTION").iterator().next();
        assertEquals("300", mapResolution);
    }

    @Test
    public void testMakeWmsGetLayerRequestDpiMapServerSet() throws Exception {
        WmsLayerParam wmsLayerParams = new WmsLayerParam();
        wmsLayerParams.layers = new String[]{"layer1", "layer2", "layer3", "layer4", "layer5"};
        wmsLayerParams.imageFormat = "image/png";
        wmsLayerParams.version = "1.1.1";
        wmsLayerParams.serverType = ServerType.MAPSERVER;
        wmsLayerParams.customParams =
                new PJsonObject(new JSONObject("{\"map_resolution\":254}"), "customParams");
        URI commonURI = new URI("http://test.xyz/geoserver/wms?SERVICE=WMS");
        Dimension imageSize = new Dimension(200, 300);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 10, 40, 50, CRS.decode("EPSG:4326"));
        final URI wmsURI = WmsUtilities.makeWmsGetLayerRequest(
                wmsLayerParams, commonURI, imageSize, 300.0, 0.0, env);
        final Multimap<String, String> finalParams = URIUtils.getParameters(wmsURI);
        assertFalse(finalParams.containsKey("MAP_RESOLUTION"));
        final String mapResolution = finalParams.get("map_resolution").iterator().next();
        assertEquals("254", mapResolution);
    }

    @Test
    public void testMakeWmsGetLayerRequestDpiGeoServer() throws Exception {
        WmsLayerParam wmsLayerParams = new WmsLayerParam();
        wmsLayerParams.layers = new String[]{"layer1", "layer2", "layer3", "layer4", "layer5"};
        wmsLayerParams.imageFormat = "image/png";
        wmsLayerParams.version = "1.1.1";
        wmsLayerParams.serverType = ServerType.GEOSERVER;
        URI commonURI = new URI("http://test.xyz/geoserver/wms?SERVICE=WMS");
        Dimension imageSize = new Dimension(200, 300);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 10, 40, 50, CRS.decode("EPSG:4326"));
        final URI wmsURI = WmsUtilities.makeWmsGetLayerRequest(
                wmsLayerParams, commonURI, imageSize, 300.0, 0.0, env);
        final Multimap<String, String> finalParams = URIUtils.getParameters(wmsURI);
        final String mapResolution = finalParams.get("FORMAT_OPTIONS").iterator().next();
        assertEquals("dpi:300", mapResolution);
    }

    @Test
    public void testMakeWmsGetLayerRequestDpiGeoServerSet() throws Exception {
        WmsLayerParam wmsLayerParams = new WmsLayerParam();
        wmsLayerParams.layers = new String[]{"layer1", "layer2", "layer3", "layer4", "layer5"};
        wmsLayerParams.customParams =
                new PJsonObject(new JSONObject("{\"format_options\":\"dpi:254\"}"), "customParams");
        wmsLayerParams.imageFormat = "image/png";
        wmsLayerParams.version = "1.1.1";
        wmsLayerParams.serverType = ServerType.GEOSERVER;
        URI commonURI = new URI("http://test.xyz/geoserver/wms?SERVICE=WMS");
        Dimension imageSize = new Dimension(200, 300);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 10, 40, 50, CRS.decode("EPSG:4326"));
        final URI wmsURI = WmsUtilities.makeWmsGetLayerRequest(
                wmsLayerParams, commonURI, imageSize, 300.0, 0.0, env);
        final Multimap<String, String> finalParams = URIUtils.getParameters(wmsURI);
        assertFalse(finalParams.containsKey("FORMAT_OPTIONS"));
        final String mapResolution = finalParams.get("format_options").iterator().next();
        assertEquals("dpi:254", mapResolution);
    }

    @Test
    public void testMakeWmsGetLayerRequestDpiGeoServerAdd() throws Exception {
        WmsLayerParam wmsLayerParams = new WmsLayerParam();
        wmsLayerParams.layers = new String[]{"layer1", "layer2", "layer3", "layer4", "layer5"};
        wmsLayerParams.customParams =
                new PJsonObject(new JSONObject("{\"format_options\":\"antialiasing:on\"}"), "customParams");
        wmsLayerParams.imageFormat = "image/png";
        wmsLayerParams.version = "1.1.1";
        wmsLayerParams.serverType = ServerType.GEOSERVER;
        URI commonURI = new URI("http://test.xyz/geoserver/wms?SERVICE=WMS");
        Dimension imageSize = new Dimension(200, 300);
        ReferencedEnvelope env = new ReferencedEnvelope(0, 10, 40, 50, CRS.decode("EPSG:4326"));
        final URI wmsURI = WmsUtilities.makeWmsGetLayerRequest(
                wmsLayerParams, commonURI, imageSize, 300.0, 0.0, env);
        final Multimap<String, String> finalParams = URIUtils.getParameters(wmsURI);
        assertFalse(finalParams.containsKey("FORMAT_OPTIONS"));
        final String mapResolution = finalParams.get("format_options").iterator().next();
        assertEquals("antialiasing:on;dpi:300", mapResolution);
    }
}
