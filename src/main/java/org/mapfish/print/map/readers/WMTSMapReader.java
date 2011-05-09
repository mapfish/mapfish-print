/*
 * Copyright (C) 2011  Swisstopo
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
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Support for the protocol using directly the content of a WMTS REST structure.
 */
public class WMTSMapReader extends TileableMapReader {
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
    private final int zoomOffset;
    private final PJsonArray matrixIds;
    private final String formatSuffix;
    

    private WMTSMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.layer = layer;
        PJsonArray maxExtent = params.optJSONArray("tileFullExtent", params.getJSONArray("maxExtent"));
        PJsonArray tileSize = params.getJSONArray("tileSize");
        opacity = params.optFloat("opacity", 1.0F);
        version = params.getString("version");
        requestEncoding = params.getString("requestEncoding");
        // Optional (but mandatory until matrixIds is supported)
        tileOrigin = params.getJSONArray("tileOrigin");
        style = params.getString("style"); 
        // Optional
        dimensions = params.optJSONArray("dimensions");
        // Optional
        dimensionsParams = params.optJSONObject("params");
        matrixSet = params.getString("matrixSet");
        // Optional (but mandatory until matrixIds is supported)
        zoomOffset = params.getInt("zoomOffset");
        // Optional
        matrixIds = params.optJSONArray("matrixIds");
        if (matrixIds != null) {
        	throw new RuntimeException("matrixIds are not supported for now. Use zoomOffset and tileOrigin instead. Patch welcome.");
        }
        formatSuffix = params.getString("formatSuffix");
        
        tileCacheLayerInfo = new WMTSLayerInfo(params.getJSONArray("resolutions"), tileSize.getInt(0), tileSize.getInt(1), maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3), formatSuffix);
    }

    protected TileRenderer.Format getFormat() {
        return TileRenderer.Format.BITMAP;
    }

    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        //not much query params for this protocol...
    }

    protected URI getTileUri(URI commonUri, Transformer transformer, float minGeoX, float minGeoY, float maxGeoX, float maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException {
        float targetResolution = (maxGeoX - minGeoX) / w;
        WMTSLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);

        int col = (int) Math.round(Math.floor((minGeoX-tileOrigin.getFloat(0)) / (resolution.value * w)));
        int row = (int) Math.round(Math.floor((tileOrigin.getFloat(1)-minGeoY) / (resolution.value * h)));
        
        StringBuilder path = new StringBuilder();
        if (!commonUri.getPath().endsWith("/")) {
            path.append('/');
        }
        if (requestEncoding.compareTo("REST") == 0) {
        	path.append("wmts");
        	path.append('/').append(version);
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
        } else {
        	throw new RuntimeException("Only WMTS REST structure is supported");
        }
    }

    protected static void create(List<MapReader> target, RenderingContext context, PJsonObject params) {
    	String layer = params.getString("layer");
    	target.add(new WMTSMapReader(layer, context, params));
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