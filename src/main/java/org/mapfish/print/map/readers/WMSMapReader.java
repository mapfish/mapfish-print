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

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.StringUtils;
import org.pvalsecc.misc.URIUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Support for the WMS protocol with possibilities to go through a WMS-C service
 * (TileCache).
 */
public class WMSMapReader extends TileableMapReader {
    private final String format;
    protected final List<String> layers = new ArrayList<String>();

    private final List<String> styles = new ArrayList<String>();

    private WMSMapReader(String layer, String style, RenderingContext context, PJsonObject params) {
        super(context, params);
        layers.add(layer);
        tileCacheLayerInfo = WMSServerInfo.getInfo(baseUrl, context).getTileCacheLayer(layer);
        styles.add(style);
        format = params.getString("format");
    }

    protected TileRenderer.Format getFormat() {
        if (format.equals("image/svg+xml")) {
            return TileRenderer.Format.SVG;
        } else if (format.equals("application/pdf") || format.equals("application/x-pdf")) {
            return TileRenderer.Format.PDF;
        } else {
            return TileRenderer.Format.BITMAP;
        }
    }

    public void render(Transformer transformer, ParallelMapTileLoader parallelMapTileLoader, String srs, boolean first) {
        PJsonObject customParams = params.optJSONObject("customParams");
        if (customParams != null) {
            // native WMS rotation - only works in singleTile mode
            if (customParams.optString("angle") != null) {
                transformer.setRotation(0);
            }
        }

        super.render(transformer, parallelMapTileLoader, srs, first);
    }

    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        URIUtils.addParamOverride(result, "FORMAT", format);
        URIUtils.addParamOverride(result, "LAYERS", StringUtils.join(layers, ","));
        URIUtils.addParamOverride(result, "SRS", srs);
        URIUtils.addParamOverride(result, "SERVICE", "WMS");
        URIUtils.addParamOverride(result, "REQUEST", "GetMap");
        //URIUtils.addParamOverride(result, "EXCEPTIONS", "application/vnd.ogc.se_inimage");
        URIUtils.addParamOverride(result, "VERSION", "1.1.1");
        if (!first) {
            URIUtils.addParamOverride(result, "TRANSPARENT", "true");
        }
        URIUtils.addParamOverride(result, "STYLES", StringUtils.join(styles, ","));
        URIUtils.addParamOverride(result, "format_options", "dpi:" + transformer.getDpi());
    }

    protected static void create(List<MapReader> target, RenderingContext context, PJsonObject params) {
        PJsonArray layers = params.getJSONArray("layers");
        PJsonArray styles = params.optJSONArray("styles");
        for (int i = 0; i < layers.size(); i++) {
            String layer = layers.getString(i);
            String style = "";
            if (styles != null && i < styles.size()) {
                style = styles.getString(i);
            }
            target.add(new WMSMapReader(layer, style, context, params));
        }
    }

    public boolean testMerge(MapReader other) {
        if (canMerge(other)) {
            WMSMapReader wms = (WMSMapReader) other;
            layers.addAll(wms.layers);
            styles.addAll(wms.styles);
            return true;
        } else {
            return false;
        }
    }

    public boolean canMerge(MapReader other) {
        if (!super.canMerge(other)) {
            return false;
        }

        if (tileCacheLayerInfo != null && !context.getConfig().isTilecacheMerging()) {
            //no layer merge when tilecache is here and we are not configured to support it...
            return false;
        }

        if (other instanceof WMSMapReader) {
            WMSMapReader wms = (WMSMapReader) other;
            if (!format.equals(wms.format)) {
                return false;
            }

            if (tileCacheLayerInfo != null && wms.tileCacheLayerInfo != null) {
                if (!tileCacheLayerInfo.equals(wms.tileCacheLayerInfo)) {
                    //not the same tilecache config
                    return false;
                }
            } else if ((tileCacheLayerInfo == null) != (wms.tileCacheLayerInfo == null)) {
                //one layer has a tilecache config and not the other?!?!? Weird...
                LOGGER.warn("Between [" + this + "] and [" + wms + "], one has a tilecache config and not the other");
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    protected URI getTileUri(URI commonUri, Transformer transformer, float minGeoX, float minGeoY, float maxGeoX, float maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException {

        Map<String, List<String>> tileParams = new HashMap<String, List<String>>();
        if (format.equals("image/svg+xml")) {
            URIUtils.addParamOverride(tileParams, "WIDTH", Long.toString(transformer.getRotatedSvgW()));
            URIUtils.addParamOverride(tileParams, "HEIGHT", Long.toString(transformer.getRotatedSvgH()));
        } else {
            URIUtils.addParamOverride(tileParams, "WIDTH", Long.toString(w));
            URIUtils.addParamOverride(tileParams, "HEIGHT", Long.toString(h));
        }
        URIUtils.addParamOverride(tileParams, "BBOX", String.format("%s,%s,%s,%s", minGeoX, minGeoY, maxGeoX, maxGeoY));
        return URIUtils.addParams(commonUri, tileParams, OVERRIDE_ALL);
    }

    public String toString() {
        return StringUtils.join(layers, ", ");
    }
}
