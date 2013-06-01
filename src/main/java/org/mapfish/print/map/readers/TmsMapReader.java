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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

public class TmsMapReader extends TileableMapReader {
	public static class Factory implements MapReaderFactory {
		@Override
		public List<? extends MapReader> create(String type, RenderingContext context,
				PJsonObject params) {
			return Collections.singletonList(new TmsMapReader("t", context, params));
		}
    }
	
    protected final String layer;
    private final String format;
    private final String extension;
    private final String layerName;
    private final String serviceVersion;

    protected TmsMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.layer = layer;
        PJsonArray maxExtent = params.getJSONArray("maxExtent");
        PJsonArray tileSize = params.getJSONArray("tileSize");
        format = params.getString("format");
        serviceVersion = "1.0.0";
        int formatSemicolon = format.indexOf(";");
        if(formatSemicolon > 0) {
          extension = format.substring(0,formatSemicolon).trim();
        } else {
          extension = format.trim();
        }
        layerName = params.getString("layer");

        PJsonObject tileOrigin = ( params.has("tileOrigin") ? params.getJSONObject("tileOrigin") : params.optJSONObject("origin") );
        final float originX;
        final float originY ;

        if(tileOrigin == null || (!tileOrigin.has("x") && !tileOrigin.has("lon"))){
            originX = 0.0f;
        }else{
            originX = tileOrigin.has("x") ? tileOrigin.getFloat("x") : tileOrigin.getFloat("lon");
        }
        if(tileOrigin == null || (!tileOrigin.has("y") && !tileOrigin.has("lat"))){
            originY = 0.0f;
        }else{
            originY = tileOrigin.has("y") ? tileOrigin.getFloat("y") : tileOrigin.getFloat("lat");
        }

        tileCacheLayerInfo = new TmsLayerInfo(params.getJSONArray("resolutions"), tileSize.getInt(0), tileSize.getInt(1), maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3), extension, originX, originY);
    }

    protected TileRenderer.Format getFormat() {
        return TileRenderer.Format.BITMAP;
    }

    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        //not much query params for this protocol...
    }

    protected URI getTileUri(URI commonUri, Transformer transformer, float minGeoX, float minGeoY, float maxGeoX, float maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException {
        float targetResolution = (maxGeoX - minGeoX) / w;
        TmsLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);

        int tileX = Math.round((minGeoX - tileCacheLayerInfo.getMinX()) / (resolution.value * w));
        int tileY = Math.round((minGeoY - tileCacheLayerInfo.getMinY()) / (resolution.value * h));


        StringBuilder path = new StringBuilder();
        if (!commonUri.getPath().endsWith("/")) {
            path.append('/');
        }

        path.append(this.serviceVersion);
        path.append('/').append(this.layerName);
        path.append('/').append(String.format("%02d", resolution.index));
        path.append('/').append(tileX);
        path.append('/').append(tileY);
        path.append('.').append(this.format);

        return new URI(commonUri.getScheme(), commonUri.getUserInfo(), commonUri.getHost(), commonUri.getPort(), commonUri.getPath() + path, commonUri.getQuery(), commonUri.getFragment());
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
