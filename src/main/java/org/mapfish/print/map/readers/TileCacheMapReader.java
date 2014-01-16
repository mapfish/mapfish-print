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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

/**
 * Support for the protocol using directly the content of a TileCache directory.
 */
public class TileCacheMapReader extends TileableMapReader {
    public static class Factory implements MapReaderFactory {
        @Override
        public List<MapReader> create(String type, RenderingContext context,
                PJsonObject params) {
            ArrayList<MapReader> target = new ArrayList<MapReader>();

            String layer = params.getString("layer");
            target.add(new TileCacheMapReader(layer, context, params));

            return target;
        }

    }

    private final String layer;

    private TileCacheMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.layer = layer;
        PJsonArray maxExtent = params.getJSONArray("maxExtent");
        PJsonArray tileSize = params.getJSONArray("tileSize");
        tileCacheLayerInfo = new TileCacheLayerInfo(params.getJSONArray("resolutions"), tileSize.getInt(0), tileSize.getInt(1), maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3), params.getString("extension"));
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
    protected URI getTileUri(URI commonUri, Transformer transformer, double minGeoX, double minGeoY, double maxGeoX, double maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException {
        double targetResolution = (maxGeoX - minGeoX) / w;
        TileCacheLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);

        int tileX = (int) Math.round((minGeoX - tileCacheLayerInfo.getMinX()) / (resolution.value * w));
        int tileY = (int) Math.round((minGeoY - tileCacheLayerInfo.getMinY()) / (resolution.value * h));

        StringBuilder path = new StringBuilder();
        if (!commonUri.getPath().endsWith("/")) {
            path.append('/');
        }
        path.append(layer);
        path.append('/').append(String.format("%02d", resolution.index));
        path.append('/').append(String.format("%03d", (tileX / 1000000) % 1000));
        path.append('/').append(String.format("%03d", (tileX / 1000) % 1000));
        path.append('/').append(String.format("%03d", tileX % 1000));
        path.append('/').append(String.format("%03d", (tileY / 1000000) % 1000));
        path.append('/').append(String.format("%03d", (tileY / 1000) % 1000));
        path.append('/').append(String.format("%03d", tileY % 1000));
        path.append('.').append(tileCacheLayerInfo.getExtension());

        return new URI(commonUri.getScheme(), commonUri.getUserInfo(), commonUri.getHost(), commonUri.getPort(), commonUri.getPath() + path, commonUri.getQuery(), commonUri.getFragment());
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
}