/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.readers;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.util.DOMUtil;
import org.mapfish.print.RenderingContext;
import org.pvalsecc.misc.URIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Use to get information about a WMS server. Caches the results.
 */
public class WMSServerInfo {
    private static final Log LOGGER = LogFactory.getLog(WMSServerInfo.class);
    private static final Map<URI, WMSServerInfo> cache = Collections.synchronizedMap(new HashMap<URI, WMSServerInfo>());

    private static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    static {
        documentBuilderFactory.setValidating(false);  //doesn't work?!?!?
    }

    /**
     * Not null if we actually use a TileCache server.
     */
    private Map<String, TileCacheLayerInfo> tileCacheLayers = null;

    public WMSServerInfo() {
    }

    public static synchronized void clearCache() {
        cache.clear();
    }

    public static WMSServerInfo getInfo(URI uri, RenderingContext context) {
        WMSServerInfo result = cache.get(uri);
        if (result == null) {
            return getInfoImpl(uri, context);
        }
        return result;
    }

    private static synchronized WMSServerInfo getInfoImpl(URI uri, RenderingContext context) {
        WMSServerInfo result = cache.get(uri);
        if (result == null) {
            try {
                result = requestInfo(uri, context);
            } catch (Exception e) {
                LOGGER.info("Error while getting capabilities for "+uri+". The print module will assume it's a standard WMS.");
                result = new WMSServerInfo();
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("GetCapabilities " + uri + ": " + result);
            }
            cache.put(uri, result);
        }
        return result;
    }

    private static WMSServerInfo requestInfo(URI baseUrl, RenderingContext context) throws IOException, URISyntaxException, ParserConfigurationException, SAXException {
        Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
        URIUtils.addParamOverride(queryParams, "SERVICE", "WMS");
        URIUtils.addParamOverride(queryParams, "REQUEST", "GetCapabilities");
        URIUtils.addParamOverride(queryParams, "VERSION", "1.1.1");
        URL url = URIUtils.addParams(baseUrl, queryParams, HTTPMapReader.OVERRIDE_ALL).toURL();

        GetMethod method = new GetMethod(url.toString());
        if (context.getReferer() != null) {
            method.setRequestHeader("Referer", context.getReferer());
        }
        try {
            context.getConfig().getHttpClient(baseUrl).executeMethod(method);
            int code = method.getStatusCode();
            if (code < 200 || code >= 300) {
                throw new IOException("Error " + code + " while reading the Capabilities from " + url + ": " + method.getStatusText());
            }
            InputStream stream = method.getResponseBodyAsStream();
            final WMSServerInfo result;
            try {
                result = parseCapabilities(stream);
            } finally {
                stream.close();
            }
            return result;
        } finally {
            method.releaseConnection();
        }
    }

    protected static WMSServerInfo parseCapabilities(InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        //we don't want the DTD to be checked and it's the only way I found
        documentBuilder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        });

        Document doc = documentBuilder.parse(stream);

        NodeList tileSets = doc.getElementsByTagName("TileSet");
        boolean isTileCache = (tileSets.getLength() > 0);

        final WMSServerInfo result = new WMSServerInfo();

        if (isTileCache) {
            result.tileCacheLayers = new HashMap<String, TileCacheLayerInfo>();

            NodeList layers = doc.getElementsByTagName("Layer");
            for (int i = 0; i < tileSets.getLength(); ++i) {
                final Node tileSet = tileSets.item(i);
                final Node layer = layers.item(i + 1);
                String resolutions = DOMUtil.getChildText(DOMUtil.getFirstChildElement(tileSet, "Resolutions"));
                int width = Integer.parseInt(DOMUtil.getChildText(DOMUtil.getFirstChildElement(tileSet, "Width")));
                int height = Integer.parseInt(DOMUtil.getChildText(DOMUtil.getFirstChildElement(tileSet, "Height")));
                Element bbox = DOMUtil.getFirstChildElement(layer, "BoundingBox");
                float minX = Float.parseFloat(DOMUtil.getAttrValue(bbox, "minx"));
                float minY = Float.parseFloat(DOMUtil.getAttrValue(bbox, "miny"));
                float maxX = Float.parseFloat(DOMUtil.getAttrValue(bbox, "maxx"));
                float maxY = Float.parseFloat(DOMUtil.getAttrValue(bbox, "maxy"));
                String format = DOMUtil.getChildText(DOMUtil.getFirstChildElement(tileSet, "Format"));

                String layerName = DOMUtil.getChildText(DOMUtil.getFirstChildElement(layer, "Name"));
                final TileCacheLayerInfo info = new TileCacheLayerInfo(resolutions, width, height, minX, minY, maxX, maxY, format);
                result.tileCacheLayers.put(layerName, info);
            }
        }

        return result;
    }

    public boolean isTileCache() {
        return tileCacheLayers != null;
    }

    public TileCacheLayerInfo getTileCacheLayer(String layerName) {
        return tileCacheLayers != null ? tileCacheLayers.get(layerName) : null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("WMSServerInfo");
        sb.append("{tileCacheLayers=").append(tileCacheLayers);
        sb.append('}');
        return sb.toString();
    }
}
