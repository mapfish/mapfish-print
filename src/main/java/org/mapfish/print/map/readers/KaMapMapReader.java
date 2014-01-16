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
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

/**
 * Support for the protocol using the KaMap tiling method
 */
public class KaMapMapReader extends TileableMapReader {
    public static class Factory implements MapReaderFactory {

        @Override
        public List<MapReader> create(String type, RenderingContext context,
                PJsonObject params) {
            ArrayList<MapReader> target = new ArrayList<MapReader>();

            String map = params.getString("map");
            String group = "";
            if (params.has("group")) {
                group = params.getString("group");
            }
            String units = context.getGlobalParams().getString("units");

            target.add(new KaMapMapReader(map, group, units, context, params));

            return target;
        }
    }

    private final String map;
    private final String group;
    private final String units;

    private KaMapMapReader(String map, String group, String units, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.map = map;
        this.group = group;
        this.units = units;

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

        StringBuilder path = new StringBuilder();

        // group (g), optional
        if (group != "") {
            path.append("g=").append(group).append('&');
        }

        // map
        path.append("map=").append(map).append('&');

        // extension (i)
        path.append("i=").append(tileCacheLayerInfo.getExtension()).append('&');

        // scale (s) calculated from units used
        final DistanceUnit unitEnum = DistanceUnit.fromString(units);
        if (unitEnum == null) {
            throw new RuntimeException("Unknown unit: '" + units + "'");
        }
        final double scale = context.getConfig().getBestScale(Math.max(
            (maxGeoX - minGeoX) / (DistanceUnit.PT.convertTo(w, unitEnum)),
            (maxGeoY- minGeoY) / (DistanceUnit.PT.convertTo(h, unitEnum))));
        path.append("s=").append(scale).append('&');

        // top & left (t & l)
        long pX = Math.round(minGeoX / resolution.value);
        long pY = Math.round(maxGeoY / resolution.value) * -1;
        pX = (long)Math.floor(pX / w) * w;
        pY = (long)Math.floor(pY / h) * h;
        path.append("l=").append(pX).append('&');
        path.append("t=").append(pY);

        return new URI(commonUri.getScheme(), commonUri.getUserInfo(), commonUri.getHost(), commonUri.getPort(), commonUri.getPath(), path.toString(), commonUri.getFragment());
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
        return map;
    }
}