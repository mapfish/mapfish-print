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

import com.vividsolutions.jts.geom.Envelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mapfish.print.RenderingContext;
import org.pvalsecc.misc.URIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.mapfish.print.map.readers.ServerInfoCache.ServiceInfoLoader.getTextContentOfChild;

/**
 * Use to get information about a WMS server. Caches the results.
 */
public class WMTSServiceInfo extends ServiceInfo {
    private static final Log LOGGER = LogFactory.getLog(WMTSServiceInfo.class);

    private static final ServerInfoCache<WMTSServiceInfo> cache = new ServerInfoCache<WMTSServiceInfo>(new WMSServiceInfoLoader());

    /**
     * Not null if we actually use a TileCache server.
     */
    final Map<String, WmtsCapabilitiesInfo> tileCacheLayers = new HashMap<String, WmtsCapabilitiesInfo>();

    public static synchronized void clearCache() {
        cache.clearCache();
    }

    public static WMTSServiceInfo getInfo(URI uri, RenderingContext context) {
        return cache.getInfo(uri, context);
    }

    public static WmtsCapabilitiesInfo getLayerInfo(URI uri, String layerId, RenderingContext context) {
        return getInfo(uri, context).tileCacheLayers.get(layerId);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("WMTSServerInfo");
        sb.append("{tileCacheLayers=").append(tileCacheLayers);
        sb.append('}');
        return sb.toString();
    }

    static class WMSServiceInfoLoader extends ServerInfoCache.ServiceInfoLoader<WMTSServiceInfo> {

        @Override
        public Log logger() {
            return LOGGER;
        }

        @Override
        public WMTSServiceInfo createNewErrorResult() {
            return new WMTSServiceInfo();
        }

        @Override
        public URL createURL(URI baseUrl, RenderingContext context) throws UnsupportedEncodingException, URISyntaxException,
                MalformedURLException {
            Map<String, List<String>> queryParams = new HashMap<String, List<String>>();
            URIUtils.addParamOverride(queryParams, "SERVICE", "WMTS");
            URIUtils.addParamOverride(queryParams, "REQUEST", "GetCapabilities");
            URIUtils.addParamOverride(queryParams, "VERSION", "1.0.0");
            URL url = URIUtils.addParams(baseUrl, queryParams, HTTPMapReader.OVERRIDE_ALL).toURL();

            return url;
        }

        @Override
        public WMTSServiceInfo parseInfo(InputStream stream) throws ParserConfigurationException, IOException, SAXException {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            //we don't want the DTD to be checked and it's the only way I found
            documentBuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return new InputSource(new StringReader(""));
                }
            });

            Document doc = documentBuilder.parse(stream);

            NodeList tileSets = doc.getElementsByTagName("Layer");

            final WMTSServiceInfo result = new WMTSServiceInfo();

            if (tileSets.getLength() > 0) {
                for (int i = 0; i < tileSets.getLength(); ++i) {
                    try {
                    final Element layer = (Element) tileSets.item(i);

                    String identifier = getTextContentOfChild(layer, "Identifier");
                    String title = getTextContentOfChild(layer, "Title", identifier);

                    ArrayList<String> formats = getTextContentOfChildren(layer, "Format");
                    final String[] lowerCorner = getTextContextFromPath(layer, "WGS84BoundingBox/LowerCorner", "-180 -90").split(" ");
                    final String[] upperCorner = getTextContextFromPath(layer, "WGS84BoundingBox/UpperCorner", "180 90").split(" ");
                    final Envelope bounds = new Envelope(Double.parseDouble(lowerCorner[0]), Double.parseDouble(upperCorner[0]),
                            Double.parseDouble(lowerCorner[1]), Double.parseDouble(upperCorner[1]));

                    Map<String, TileMatrixSet> tileMatrices = new HashMap<String, TileMatrixSet>();
                    final NodeList matrixSetLink = layer.getElementsByTagName("TileMatrixSetLink");
                    for (int j = 0; j < matrixSetLink.getLength(); j++) {
                        Element element = (Element) matrixSetLink.item(j);
                        final TileMatrixSet tileMatrixSet = new TileMatrixSet(element);
                        tileMatrices.put(tileMatrixSet.id, tileMatrixSet);
                    }
                    result.tileCacheLayers.put(identifier, new WmtsCapabilitiesInfo(identifier, title, formats, bounds, tileMatrices));
                    } catch (NoSuchElementException e) {
                        LOGGER.warn(e.getMessage(), e);
                    }
                }
            }

            return result;
        }

    }

    static class TileMatrixSet {
        final String id;
        final Map<String, TileMatrixLimit> limits;

        public TileMatrixSet(Element element) {
            id = getTextContentOfChild(element, "TileMatrixSet");
            Map<String, TileMatrixLimit> tmpLimits = new HashMap<String, WMTSServiceInfo.TileMatrixLimit>();
            final NodeList tileMatrixLimits = element.getElementsByTagName("TileMatrixLimits");
            for (int j = 0; j < tileMatrixLimits.getLength(); j++) {
                Element el = (Element) tileMatrixLimits.item(j);
                TileMatrixLimit limit = new WMTSServiceInfo.TileMatrixLimit(el);
                tmpLimits.put(limit.id, limit);
            }
            this.limits = Collections.unmodifiableMap(tmpLimits);
        }
    }

    static class TileMatrixLimit {
        final String id;
        final int MinTileRow;
        final int MaxTileRow;
        final int MinTileCol;
        final int MaxTileCol;


        public TileMatrixLimit(Element el) {
            id = getTextContentOfChild(el, "TileMatrix");
            MinTileRow = Integer.parseInt(getTextContentOfChild(el, "MinTileRow"));
            MaxTileRow = Integer.parseInt(getTextContentOfChild(el, "MaxTileRow"));
            MinTileCol = Integer.parseInt(getTextContentOfChild(el, "MinTileCol"));
            MaxTileCol = Integer.parseInt(getTextContentOfChild(el, "MaxTileCol"));
        }
    }
}
