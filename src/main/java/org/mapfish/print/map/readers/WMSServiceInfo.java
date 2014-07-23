/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.map.readers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.util.DOMUtil;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.URIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Use to get information about a WMS server. Caches the results.
 */
public class WMSServiceInfo extends ServiceInfo {
    private static final Log LOGGER = LogFactory.getLog(WMSServiceInfo.class);

    private static final ServerInfoCache<WMSServiceInfo> cache = new ServerInfoCache<WMSServiceInfo>(new WMSServiceInfoLoader());

    /**
     * Not null if we actually use a TileCache server.
     */
    private final Map<String, TileCacheLayerInfo> tileCacheLayers = new HashMap<String, TileCacheLayerInfo>();

    public static synchronized void clearCache() {
        cache.clearCache();
    }

    public static WMSServiceInfo getInfo(URI uri, RenderingContext context) {
        return cache.getInfo(uri, context);
    }

    public boolean isTileCache() {
        return !tileCacheLayers.isEmpty();
    }

    public TileCacheLayerInfo getTileCacheLayer(String layerName, RenderingContext context, PJsonObject params) {
        if (params.optBool("isTiled", false)) {
            int dpi = context.getCurrentPageParams().getInt("dpi");
            String units = context.getGlobalParams().getString("units");
            double invDistancePerGeoUnit = 1 / DistanceUnit.fromString(units).convertTo(dpi, DistanceUnit.IN);

            PJsonArray maxExtent = params.getJSONArray("maxExtent");
            PJsonArray tileSize = params.optJSONArray("tileSize");
            String format = params.getString("format");
            List<Double> resolutions = new ArrayList<Double>(context.getConfig().getScales().size());
            for (Number scale : context.getConfig().getScales()) {
                resolutions.add(scale.doubleValue() * invDistancePerGeoUnit);
            }
            double[] resolutionsArray = new double[resolutions.size()];
            for (int i = 0; i < resolutions.size(); i++) {
                resolutionsArray[i] = resolutions.get(i).doubleValue();
            }
            return new TileCacheLayerInfo(resolutionsArray,
                tileSize == null ? 256 : tileSize.getInt(0), tileSize == null ? 256 : tileSize.getInt(1),
                maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3),
                format);
        }
        return tileCacheLayers.get(layerName);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("WMSServerInfo");
        sb.append("{tileCacheLayers=").append(tileCacheLayers);
        sb.append('}');
        return sb.toString();
    }

    static class WMSServiceInfoLoader extends ServerInfoCache.ServiceInfoLoader<WMSServiceInfo> {

        @Override
        public Log logger() {
            return LOGGER;
        }

        @Override
        public WMSServiceInfo createNewErrorResult() {
            return new WMSServiceInfo();
        }

        @Override
        public URL createURL(URI baseUrl, RenderingContext context) throws UnsupportedEncodingException, URISyntaxException, MalformedURLException {
            Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
            URIUtils.addParamOverride(queryParams, "SERVICE", "WMS");
            URIUtils.addParamOverride(queryParams, "REQUEST", "GetCapabilities");
            URIUtils.addParamOverride(queryParams, "VERSION", "1.1.1");
            URL url = URIUtils.addParams(baseUrl, queryParams, HTTPMapReader.OVERRIDE_ALL).toURL();

            return url;
        }

        @Override
        public WMSServiceInfo parseInfo(InputStream stream) throws ParserConfigurationException, IOException, SAXException {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            //we don't want the DTD to be checked and it's the only way I found
            documentBuilder.setEntityResolver(new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return new InputSource(new StringReader(""));
                }
            });

            Document doc = documentBuilder.parse(stream);

            NodeList tileSets = doc.getElementsByTagName("TileSet");
            boolean isTileCache = (tileSets.getLength() > 0);

            final WMSServiceInfo result = new WMSServiceInfo();

            if (isTileCache) {
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
    }
}
