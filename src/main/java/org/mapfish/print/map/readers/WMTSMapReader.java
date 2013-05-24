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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

/**
 * Support for the protocol using directly the content of a WMTS tiled layer, support REST or KVP.
 */
public class WMTSMapReader extends TileableMapReader {
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
    private final String requestEncoding;
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
        // Optional (but mandatory if matrixIds is not provided)
        PJsonArray maxExtent = params.optJSONArray("tileFullExtent", params.optJSONArray("maxExtent"));
        // Optional (but mandatory if matrixIds is not provided)
        PJsonArray tileSize = params.optJSONArray("tileSize");
        opacity = params.optFloat("opacity", 1.0F);
        version = params.getString("version");
        requestEncoding = params.getString("requestEncoding");
        // Optional (but mandatory if matrixIds is not provided)
        tileOrigin = params.optJSONArray("tileOrigin");
        style = params.getString("style");
        // Optional
        dimensions = params.optJSONArray("dimensions");
        // Optional
        dimensionsParams = params.optJSONObject("params");
        matrixSet = params.getString("matrixSet");
        // Optional (but mandatory if matrixIds is not provided)
        zoomOffset = params.optInt("zoomOffset");
        // Optional
        matrixIds = params.optJSONArray("matrixIds");
        // Optional (but mandatory if matrixIds is not provided)
        formatSuffix = params.optString("formatSuffix");
        // Optional (but mandatory if matrixIds is provided and requestEncoding is KVP)
        format = params.optString("format");

        if (matrixIds == null) {
            tileCacheLayerInfo = new WMTSLayerInfo(params.getJSONArray("resolutions"), tileSize.getInt(0), tileSize.getInt(1), maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3), formatSuffix);
        }
    }

    protected TileRenderer.Format getFormat() {
        return TileRenderer.Format.BITMAP;
    }

    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        //not much query params for this protocol...
    }

    protected void renderTiles(TileRenderer formater, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException, URISyntaxException {
        if (matrixIds != null) {
            float diff = Float.POSITIVE_INFINITY;
            float targetResolution = transformer.getGeoW() / transformer.getStraightBitmapW();
            for (int i = 0 ; i < matrixIds.size() ; i++) {
                PJsonObject matrixId = matrixIds.getJSONObject(i);
                float resolution = matrixId.getFloat("resolution");
                float delta = Math.abs(1 - resolution / targetResolution);
                if (delta < diff) {
                    diff = delta;
                    matrix = matrixId;
                }
            }
            float resolution = matrix.getFloat("resolution");
            PJsonArray tileSize = matrix.getJSONArray("tileSize");
            PJsonArray topLeftCorner = matrix.getJSONArray("topLeftCorner");
            PJsonArray matrixSize = matrix.getJSONArray("matrixSize");
            tileCacheLayerInfo = new TileCacheLayerInfo(
                    String.valueOf(resolution),
                    tileSize.getInt(0), tileSize.getInt(1),
                    topLeftCorner.getFloat(0),
                    topLeftCorner.getFloat(1) - tileSize.getInt(1) * matrixSize.getInt(1) * resolution,
                    topLeftCorner.getFloat(0) + tileSize.getInt(0) * matrixSize.getInt(0) * resolution,
                    topLeftCorner.getFloat(1),
                    format);
        }
        super.renderTiles(formater, transformer, commonUri, parallelMapTileLoader);
    }

    protected URI getTileUri(URI commonUri, Transformer transformer, float minGeoX, float minGeoY, float maxGeoX, float maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException {
        if (matrixIds != null) {
            PJsonArray topLeftCorner = matrix.getJSONArray("topLeftCorner");
            float factor = 1 / (matrix.getFloat("resolution") * w);
            int row = (int)Math.round((topLeftCorner.getFloat(1) - maxGeoY) * factor);
            int col = (int)Math.round((minGeoX - topLeftCorner.getFloat(0)) * factor);
            if ("REST".equals(requestEncoding)) {
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
            float targetResolution = (maxGeoX - minGeoX) / w;
            WMTSLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);

            int col = (int) Math.round(Math.floor(((maxGeoX + minGeoX)/2-tileOrigin.getFloat(0)) / (resolution.value * w)));
            int row = (int) Math.round(Math.floor((tileOrigin.getFloat(1)-(maxGeoY + minGeoY)/2) / (resolution.value * h)));

            StringBuilder path = new StringBuilder();
            if (!commonUri.getPath().endsWith("/")) {
                path.append('/');
            }
            if (requestEncoding.compareTo("REST") == 0) {
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
                query += "&TILEMATRIX=" + (resolution.index + zoomOffset);
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

    public boolean testMerge(MapReader other) {
        return false;
    }

    public boolean canMerge(MapReader other) {
        return false;
    }

    public String toString() {
        return layer;
    }
}
