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

package org.mapfish.print.map.readers.google;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.readers.HTTPMapReader;
import org.mapfish.print.map.readers.MapReader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.URIUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleMapReader extends HTTPMapReader {

    private final String layer;
    private final GoogleConfig config;
    private final GoogleLayerInfo layerInfo;

    private GoogleMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.layer = layer;
        config = new GoogleConfig(context,params,LOGGER, baseUrl, true);
        // width and height are not important as they are not used by this reader
        PJsonArray maxExtent = params.getJSONArray("maxExtent");
        layerInfo = new GoogleLayerInfo(params.getJSONArray("resolutions"), 640, 640, maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3), params.getString("extension"));
    }

    protected void renderTiles(TileRenderer formatter, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException, URISyntaxException {
        float maxGeoX = transformer.getRotatedMaxGeoX();
        float minGeoX = transformer.getRotatedMinGeoX();
        float maxGeoY = transformer.getRotatedMaxGeoY();
        float minGeoY = transformer.getRotatedMinGeoY();
        long width = transformer.getRotatedBitmapW();
        long height = transformer.getRotatedBitmapH();
        
        float targetResolution = transformer.getGeoW() / width;
        GoogleLayerInfo.ResolutionInfo resolution = layerInfo.getNearestResolution(targetResolution);

        Map<String, List<String>> tileParams = new HashMap<String, List<String>>();

        // Geometry transformation from 900913 to lat/lon
        // See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
        double latitude;
        double longitude;
        double originShift = 20037508.342789244;

        longitude = (((maxGeoX + minGeoX) / 2.0) / originShift) * 180.0;
        latitude = (((maxGeoY + minGeoY) / 2.0) / originShift) * 180.0;

        latitude = 180 / Math.PI * (2 * Math.atan( Math.exp( latitude * Math.PI / 180.0)) - Math.PI / 2.0);
        DecimalFormat df = new DecimalFormat("#.#######################");
        String center = df.format(latitude) + "," + df.format(longitude);
        String size = Long.toString(width) + "x" + Long.toString(height);

        URIUtils.addParamOverride(tileParams, "center", center);
        URIUtils.addParamOverride(tileParams, "size", size);
        URIUtils.addParamOverride(tileParams, "zoom", Integer.toString(resolution.index));
        URI uri = URIUtils.addParams(commonUri, tileParams, OVERRIDE_ALL);


        //tiling not supported and not really needed (tilecache doesn't support this protocol) for MapServer protocol...
        List<URI> uris = new ArrayList<URI>(1);
        uris.add(config.signURI(uri));
        formatter.render(transformer, uris, parallelMapTileLoader, context, opacity, 1, 0, 0,
                transformer.getRotatedBitmapW(), transformer.getRotatedBitmapH());
    }

    protected TileRenderer.Format getFormat() {
        return TileRenderer.Format.BITMAP;
    }

    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        URIUtils.addParamOverride(result, "client", config.signer.clientId());
        URIUtils.addParamOverride(result, "sensor", config.sensor);
        URIUtils.addParamOverride(result, "format", config.format);
        URIUtils.addParamOverride(result, "maptype", config.maptype);
        if(config.language != null) URIUtils.addParamOverride(result, "language", config.language);
        if(config.markers.size() > 0) result.put("markers", config.markers);
        if(config.path != null) URIUtils.addParamOverride(result, "path", config.path);
    }

    public static void create(List<MapReader> target, RenderingContext context, PJsonObject params) {
        target.add(new GoogleMapReader("t", context, params));
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
