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

import org.apache.log4j.Logger;
import org.json.JSONException;
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

    private boolean strictEpsg4326;

    enum WMSVersion {
        VERSION1_1_1("1.1.1"), VERSION1_3_0("1.3.0");
        public final String code;

        WMSVersion(String code) {
            this.code = code;
        }

        public static WMSVersion find(String version) {
            for (WMSVersion wmsVersion : values()) {
                if (wmsVersion.code.equals(version)) {
                    return wmsVersion;
                }
            }

            throw new IllegalArgumentException("WMS Version: "+version+" is not a supported version");
        }

    }

    private static final WMSVersion DEFAULT_VERSION = WMSVersion.VERSION1_1_1;

    public static class Factory implements MapReaderFactory {
        @Override
        public List<MapReader> create(String type, RenderingContext context, PJsonObject params) {
            ArrayList<MapReader> target = new ArrayList<MapReader>();
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

            return target;
        }
    }

    public static final Logger LOGGER = Logger.getLogger(WMSMapReader.class);
    private final String format;
    protected final List<String> layers = new ArrayList<String>();
    private WMSVersion version;
    private final List<String> styles = new ArrayList<String>();

    private WMSMapReader(String layer, String style, RenderingContext context, PJsonObject params) {
        super(context, params);
        layers.add(layer);
        if (!context.getConfig().isIgnoreCapabilities()) {
            tileCacheLayerInfo = WMSServiceInfo.getInfo(baseUrl, context).getTileCacheLayer(layer);
        }
        styles.add(style);
        format = params.getString("format");
        version = WMSVersion.find(params.optString("version", DEFAULT_VERSION.code));
        final PJsonObject customParams = params.optJSONObject("customParams");

        if (customParams != null) {
            version = WMSVersion.find(customParams.optString("version", version.code));
        }

        if (version == WMSVersion.VERSION1_3_0 &&
            params.optString("srs", context.getGlobalParams().optString("srs", "CRS:4326")).equals("EPSG:4326")) {
            strictEpsg4326 = true;
        }
    }
    
    @Override
    protected String getMergeableValue(PJsonObject customParams,
            final List<String> toBeSkipped, String key) throws JSONException {
        // adds support for an array of values for mergeable parameters
        // so that a single value can be specified for each single layer
        if(customParams.optJSONArray(key) != null) {
            String value = null;
            PJsonArray listOfValues = customParams.getJSONArray(key);
            int notUsed = -1;
            for(int count = 0; count< listOfValues.size() && notUsed == -1; count++) {
                value = listOfValues.getInternalArray().optString(count, null);
                if(value != null) {
                    notUsed = count;
                    listOfValues.getInternalArray().put(count, (String)null);
                }
            }
            if(notUsed == (listOfValues.size() - 1)) {
                toBeSkipped.add(key);
            }
            return value;
        } else {
            return super.getMergeableValue(customParams, toBeSkipped, key);
        }
    }
    
    @Override
    protected TileRenderer.Format getFormat() {
        if (format.equals("image/svg+xml")) {
            return TileRenderer.Format.SVG;
        } else if (format.equals("application/pdf") || format.equals("application/x-pdf")) {
            return TileRenderer.Format.PDF;
        } else {
            return TileRenderer.Format.BITMAP;
        }
    }
    @Override
    public void render(Transformer transformer, ParallelMapTileLoader parallelMapTileLoader, String srs, boolean first) {
        PJsonObject customParams = params.optJSONObject("customParams");

        // store the rotation to not change for other layers
        double oldAngle = transformer.getRotation();

        // native WMS rotation - only works in singleTile mode
        if (customParams != null && customParams.optString("angle") != null) { // For GeoServer
            transformer.setRotation(0);
        }
        if (params.optBool("useNativeAngle", false)) {
            String angle = String.valueOf(-Math.toDegrees(transformer.getRotation()));
            try {
                if (customParams != null) {
                    customParams.getInternalObj().put("angle", angle); // For GeoServer
                    customParams.getInternalObj().put("map_angle", angle); // For MapServer
                } else {
                    Map<String, String> customMap = new HashMap<String, String>();
                    customMap.put("angle", angle); // For GeoServer
                    customMap.put("map_angle", angle); // For MapServer
                    params.getInternalObj().put("customParams", customMap);
                }
                transformer.setRotation(0);
            } catch (org.json.JSONException e) {
                LOGGER.error("Unable to set angle: " + e.getClass().getName() + " - " + e.getMessage());
            }
        }
        super.render(transformer, parallelMapTileLoader, srs, first);
        // restore the rotation for other layers
        transformer.setRotation(oldAngle);
    }
    @Override
    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        URIUtils.setParamDefault(result, "FORMAT", format);
        URIUtils.setParamDefault(result, "LAYERS", StringUtils.join(layers, ","));
        URIUtils.setParamDefault(result, "SERVICE", "WMS");
        URIUtils.setParamDefault(result, "REQUEST", "GetMap");

        final String versionParamName = "VERSION";

        switch (version) {
            case VERSION1_3_0:
                URIUtils.setParamDefault(result, "CRS", srs);
                URIUtils.setParamDefault(result, versionParamName, version.code);
                break;
            default:
                URIUtils.setParamDefault(result, "SRS", srs);
                URIUtils.setParamDefault(result, versionParamName, WMSVersion.VERSION1_1_1.code);
        }

        if (!first) {
            URIUtils.setParamDefault(result, "TRANSPARENT", "true");
        }
        URIUtils.setParamDefault(result, "STYLES", StringUtils.join(styles, ","));
        URIUtils.setParamDefault(result, "format_options", "dpi:" + transformer.getDpi()); // For GeoServer
        URIUtils.setParamDefault(result, "map_resolution", String.valueOf(transformer.getDpi())); // For MapServer
        URIUtils.setParamDefault(result, "DPI", String.valueOf(transformer.getDpi())); // For QGIS mapserver

    }
    @Override
    public boolean testMerge(MapReader other) {
        if (!context.getConfig().isDisableLayersMerging() && canMerge(other)) {
            WMSMapReader wms = (WMSMapReader) other;
            layers.addAll(wms.layers);
            styles.addAll(wms.styles);
            return super.testMerge(other);
        } else {
            return false;
        }
    }
    @Override
    public boolean canMerge(MapReader other) {
        if (!super.canMerge(other)) {
            return false;
        }

        if (tileCacheLayerInfo != null && !context.getConfig().isTilecacheMerging()) {
            //no layer merge when tile cache is here and we are not configured to support it...
            return false;
        }

        if (other instanceof WMSMapReader) {
            WMSMapReader wms = (WMSMapReader) other;
            if (!format.equals(wms.format)) {
                return false;
            }

            if (tileCacheLayerInfo != null && wms.tileCacheLayerInfo != null) {
                if (!tileCacheLayerInfo.equals(wms.tileCacheLayerInfo)) {
                    //not the same tile cache config
                    return false;
                }
            } else if ((tileCacheLayerInfo == null) != (wms.tileCacheLayerInfo == null)) {
                //one layer has a tile cache config and not the other?!?!? Weird...
                LOGGER.warn("Between [" + this + "] and [" + wms + "], one has a tile cache config and not the other");
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    @Override
    protected URI getTileUri(URI commonUri, Transformer transformer, double minGeoX, double minGeoY, double maxGeoX, double maxGeoY,
                             long w, long h) throws URISyntaxException, UnsupportedEncodingException {

        Map<String, List<String>> tileParams = new HashMap<String, List<String>>();
        if (format.equals("image/svg+xml")) {
            double maxW = context.getConfig().getMaxSvgW(); // config setting in YAML called maxSvgWidth
            double maxH = context.getConfig().getMaxSvgH(); // config setting in YAML called maxSvgHeight
            double divisor;
            double width = transformer.getRotatedSvgW(); // width of the vector map
            double height = transformer.getRotatedSvgH(); // height of the vector map

            if (maxW < width || maxH < height) {
                /**
                 *  need to use maxW as divisor, smaller quotient for width means
                 *  more constraining factor is max width
                 */
                if (maxW / maxH < width / height) {
                    //LOGGER.warn("before width="+width+" height="+height);
                    divisor = width / maxW;
                    width = maxW;
                    height = height / divisor;
                    //LOGGER.warn("after width="+width+" height="+height);
                } else {
                    //LOGGER.warn("before width="+width+" height="+height);
                    divisor = height / maxH;
                    height = maxH;
                    width = width / divisor;
                    //LOGGER.warn("after width="+width+" height="+height);
                }
            }
            URIUtils.addParamOverride(tileParams, "WIDTH", Long.toString(Math.round(width)));
            URIUtils.addParamOverride(tileParams, "HEIGHT", Long.toString(Math.round(height)));
        } else {
            URIUtils.addParamOverride(tileParams, "WIDTH", Long.toString(w));
            URIUtils.addParamOverride(tileParams, "HEIGHT", Long.toString(h));
        }

        final String bbox;

        switch (version) {
            case VERSION1_3_0:
                if (strictEpsg4326) {
                    bbox = String.format("%s,%s,%s,%s", minGeoY, minGeoX, maxGeoY, maxGeoX);
                } else {
                    bbox = String.format("%s,%s,%s,%s", minGeoX, minGeoY, maxGeoX, maxGeoY);
                }
                break;
            default:
                bbox = String.format("%s,%s,%s,%s", minGeoX, minGeoY, maxGeoX, maxGeoY);
        }

        URIUtils.addParamOverride(tileParams, "BBOX", bbox);
        return URIUtils.addParams(commonUri, tileParams, OVERRIDE_ALL);
    }
    @Override
    public String toString() {
        return StringUtils.join(layers, ", ");
    }
}
