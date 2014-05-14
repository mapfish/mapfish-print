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

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Support for the protocol using directly the content of a WMTS tiled layer, support REST or KVP.
 */
public class WMTSMapReader extends TileableMapReader {

    private static final String RESOLUTION = "resolution";
    private static final String TILE_SIZE = "tileSize";
    private static final String TOP_LEFT_CORNER = "topLeftCorner";
    private static final String MATRIX_SIZE = "matrixSize";
    private static final String RESOLUTIONS = "resolutions";
    private static final String MATRIX_IDS = "matrixIds";
    private static final String MATRIX_SET = "matrixSet";
    private static final String ZOOM_OFFSET = "zoomOffset";
    private static final String FORMAT_SUFFIX = "formatSuffix";
    private static final String EXTENSION = "extension";
    private static final String FORMAT = "format";
    private static final String TILE_ORIGIN = "tileOrigin";
    private static final String STYLE = "style";
    private static final String VERSION = "version";
    private static final String OPACITY = "opacity";
    private static final String TILE_FULL_EXTENT = "tileFullExtent";
    private static final String MAX_EXTENT = "maxExtent";
    private static final String REQUEST_ENCODING = "requestEncoding";
    private static final String DIMENSIONS = "dimensions";
    private static final String PARAMS = "params";
    private final WmtsCapabilitiesInfo capabilitiesInfo;

    public static class Factory implements MapReaderFactory {
        @Override
        public List<? extends MapReader> create(String type, RenderingContext context,
                PJsonObject params) {
            return Collections.singletonList(new WMTSMapReader(params.getString("layer"), context, params));
        }
    }

    protected final String layer;
    @SuppressWarnings("unused")
    private final float opacity;
    private final String version;
    private final WMTSRequestEncoding requestEncoding;
    private final PJsonArray tileOrigin;
    private final String style;
    private final PJsonArray dimensions;
    private final PJsonObject dimensionsParams;
    private final String matrixSet;
    private final Integer zoomOffset;
    private final PJsonArray matrixIds;
    private final String formatSuffix;
    private final String format;
    private PJsonObject matrix; // the currently used matrix


    private WMTSMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.layer = layer;
        if (!context.getConfig().isIgnoreCapabilities()) {
            this.capabilitiesInfo = WMTSServiceInfo.getLayerInfo(baseUrl, layer, context);
        } else {
            this.capabilitiesInfo = null;
        }
        // Optional (but mandatory if matrixIds is not provided)
        PJsonArray maxExtent = params.optJSONArray(TILE_FULL_EXTENT, params.optJSONArray(MAX_EXTENT));
        // Optional (but mandatory if matrixIds is not provided)
        PJsonArray tileSize = params.optJSONArray(TILE_SIZE);
        opacity = params.optFloat(OPACITY, 1.0F);
        version = params.optString(VERSION, "1.0.0");
        requestEncoding = WMTSRequestEncoding.valueOf(params.optString(REQUEST_ENCODING, WMTSRequestEncoding.REST.name()));

        // Optional (but mandatory if matrixIds is not provided)
        tileOrigin = params.optJSONArray(TILE_ORIGIN);
        style = params.optString(STYLE, "");
        // Optional
        dimensions = params.optJSONArray(DIMENSIONS);
        // Optional
        dimensionsParams = params.optJSONObject(PARAMS);
        matrixSet = params.getString(MATRIX_SET);
        // Optional (but mandatory if matrixIds is not provided)
        zoomOffset = params.optInt(ZOOM_OFFSET);
        // Optional
        matrixIds = params.optJSONArray(MATRIX_IDS);
        // Optional (but mandatory if matrixIds is not provided)
        formatSuffix = params.optString(FORMAT_SUFFIX, params.optString(EXTENSION));

        // Optional (but mandatory if matrixIds is provided and requestEncoding is KVP)
        format = params.optString(FORMAT);

        if (tileOrigin == null && matrixIds == null) {
            throw new IllegalArgumentException("Either "+TILE_ORIGIN+" or "+MATRIX_IDS+" is required.");
        }
        if (zoomOffset == null && matrixIds == null) {
            throw new IllegalArgumentException("Either "+ZOOM_OFFSET+" or "+MATRIX_IDS+" is required.");
        }
        if (formatSuffix == null && matrixIds == null) {
            throw new IllegalArgumentException("Either "+EXTENSION+" (or "+FORMAT_SUFFIX+") or "+MATRIX_IDS+" is required.");
        }
        if (matrixIds == null) {
            tileCacheLayerInfo = new WMTSLayerInfo(params.getJSONArray(RESOLUTIONS), tileSize.getInt(0), tileSize.getInt(1), maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3), formatSuffix);
        }
    }
    @Override
    protected TileRenderer.Format getFormat() {
        return TileRenderer.Format.BITMAP;
    }
    @Override
    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        //not much query params for this protocol...
    }
    @Override
    protected void renderTiles(TileRenderer formatter, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException, URISyntaxException {
        if (matrixIds != null) {
            double diff = Double.POSITIVE_INFINITY;
            double targetResolution = transformer.getGeoW() / transformer.getStraightBitmapW();
            for (int i = 0 ; i < matrixIds.size() ; i++) {
                PJsonObject matrixId = matrixIds.getJSONObject(i);
                float resolution = matrixId.getFloat(RESOLUTION);
                double delta = Math.abs(1 - resolution / targetResolution);
                if (delta < diff) {
                    diff = delta;
                    matrix = matrixId;
                }
            }
            float resolution = matrix.getFloat(RESOLUTION);
            PJsonArray tileSize = matrix.getJSONArray(TILE_SIZE);
            PJsonArray topLeftCorner = matrix.getJSONArray(TOP_LEFT_CORNER);
            PJsonArray matrixSize = matrix.getJSONArray(MATRIX_SIZE);
            tileCacheLayerInfo = new TileCacheLayerInfo(
                    String.valueOf(resolution),
                    tileSize.getInt(0), tileSize.getInt(1),
                    topLeftCorner.getFloat(0),
                    topLeftCorner.getFloat(1) - tileSize.getInt(1) * matrixSize.getInt(1) * resolution,
                    topLeftCorner.getFloat(0) + tileSize.getInt(0) * matrixSize.getInt(0) * resolution,
                    topLeftCorner.getFloat(1),
                    format);
        }
        super.renderTiles(formatter, transformer, commonUri, parallelMapTileLoader);
    }
    @Override
    protected URI getTileUri(URI commonUri, Transformer transformer, double minGeoX, double minGeoY, double maxGeoX, double maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException {
        if (matrixIds != null) {
            PJsonArray topLeftCorner = matrix.getJSONArray(TOP_LEFT_CORNER);
            float factor = 1 / (matrix.getFloat(RESOLUTION) * w);
            int row = (int) Math.round((topLeftCorner.getDouble(1) - maxGeoY) * factor);
            int col = (int) Math.round((minGeoX - topLeftCorner.getDouble(0)) * factor);
            if (WMTSRequestEncoding.REST == requestEncoding) {
                String path = commonUri.getPath();
                for (int i = 0 ; i < dimensions.size() ; i++) {
                    String d = dimensions.getString(i);
                    path = path.replace("{" + d + "}", dimensionsParams.getString(d.toUpperCase()));
                }
                path = path.replace("{TileMatrixSet}", matrixSet);
                path = path.replace("{TileMatrix}", matrix.getString("identifier"));
                path = path.replace("{TileRow}", String.valueOf(row));
                path = path.replace("{TileCol}", String.valueOf(col));

                return new URI(commonUri.getScheme(), commonUri.getUserInfo(), commonUri.getHost(), commonUri.getPort(),
                        path, commonUri.getQuery(), commonUri.getFragment());
            }
            else {
                String query = "SERVICE=WMTS";
                query += "&REQUEST=GetTile";
                query += "&VERSION=" + version;
                query += "&LAYER=" + layer;
                query += "&STYLE=" + style;
                query += "&TILEMATRIXSET=" + matrixSet;
                query += "&TILEMATRIX=" + matrix.getString("identifier");
                query += "&TILEROW=" + row;
                query += "&TILECOL=" + col;
                query += "&FORMAT=" + format;
                if (dimensions != null) {
                    for (int i = 0 ; i < dimensions.size() ; i++) {
                        String d = dimensions.getString(i);
                        query += "&" + d + "=" + dimensionsParams.getString(d.toUpperCase());
                    }
                }
                return new URI(commonUri.getScheme(), commonUri.getUserInfo(), commonUri.getHost(), commonUri.getPort(),
                        commonUri.getPath(), query, commonUri.getFragment());
            }
        }
        else {
            double targetResolution = (maxGeoX - minGeoX) / w;
            WMTSLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);

            int col = (int) Math.round(Math.floor(((maxGeoX + minGeoX)/2-tileOrigin.getDouble(0)) / (resolution.value * w)));
            int row = (int) Math.round(Math.floor((tileOrigin.getDouble(1)-(maxGeoY + minGeoY)/2) / (resolution.value * h)));

            StringBuilder path = new StringBuilder();
            if (!commonUri.getPath().endsWith("/")) {
                path.append('/');
            }
            if (requestEncoding == WMTSRequestEncoding.REST) {
                path.append(version);
                path.append('/').append(layer);
                path.append('/').append(style);
                // Add dimensions
                if (dimensions != null) {
                    for (int i = 0; i< dimensions.size(); i++) {
                        path.append('/').append(dimensionsParams.getString(dimensions.getString(i)));
                    }
                }
                path.append('/').append(matrixSet);
                path.append('/').append(resolution.index + zoomOffset);
                path.append('/').append(row);
                path.append('/').append(col);

                path.append('.').append(tileCacheLayerInfo.getExtension());

                return new URI(commonUri.getScheme(), commonUri.getUserInfo(), commonUri.getHost(), commonUri.getPort(), commonUri.getPath() + path, commonUri.getQuery(), commonUri.getFragment());
            }
            else {
                String query = "SERVICE=WMTS";
                query += "&REQUEST=GetTile";
                query += "&VERSION=" + version;
                query += "&LAYER=" + layer;
                query += "&STYLE=" + style;
                query += "&TILEMATRIXSET=" + matrixSet;
                String tileMatrix = ""+(resolution.index + zoomOffset);
                if (capabilitiesInfo != null && capabilitiesInfo.getTileMatrices().containsKey(matrixSet)) {
                    final WMTSServiceInfo.TileMatrixSet tileMatrixSet = capabilitiesInfo.getTileMatrices().get(matrixSet);
                    if (!tileMatrixSet.limits.containsKey(tileMatrix)) {
                        // try to find a tileMatrix from capabilities that seems to match parameters
                        final WMTSServiceInfo.TileMatrixLimit limit = tileMatrixSet.limits.get(matrixSet + ":" + tileMatrix);
                        if (limit != null) {
                            tileMatrix = limit.id;
                        } else {
                            for (WMTSServiceInfo.TileMatrixLimit l : tileMatrixSet.limits.values()) {
                                if (l.id.endsWith(":"+tileMatrix)) {
                                    tileMatrix = l.id;
                                    break;
                                }
                            }
                        }
                    }
                }
                query += "&TILEMATRIX=" + tileMatrix;
                query += "&TILEROW=" + row;
                query += "&TILECOL=" + col;
                query += "&FORMAT=" + (formatSuffix.equals("png") ? "image/png" : "image/jpeg");
                if (dimensions != null) {
                    for (int i = 0 ; i < dimensions.size() ; i++) {
                        String d = dimensions.getString(i);
                        query += "&" + d + "=" + dimensionsParams.getString(d.toUpperCase());
                    }
                }
                return new URI(commonUri.getScheme(), commonUri.getUserInfo(), commonUri.getHost(), commonUri.getPort(),
                        commonUri.getPath(), query, commonUri.getFragment());
            }
        }
    }
    @Override
    public boolean testMerge(MapReader other) {
        return false;
    }
    @Override
    public boolean canMerge(MapReader other) {
        return false;
    }
    @Override
    public String toString() {
        return layer;
    }

    private enum WMTSRequestEncoding {
        KVP, REST
    }
}
