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

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.readers.MapReader;
import org.mapfish.print.map.readers.TileableMapReader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.URIUtils;

/**
 * Support for the protocol using directly the content of a Google Map Static API directory.
 */
public class GoogleMapTileReader extends TileableMapReader {
    protected final String layer;
    private GoogleConfig config;


    protected GoogleMapTileReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.layer = layer;
        PJsonArray maxExtent = params.getJSONArray("maxExtent");
        tileCacheLayerInfo = new GoogleLayerInfo(params.getJSONArray("resolutions"), 640, 640, maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3), params.getString("extension"));
        config = new GoogleConfig(context,params,LOGGER, baseUrl, false);
    }

    protected TileRenderer.Format getFormat() {
        return TileRenderer.Format.BITMAP;
    }

    protected URI getTileUri(URI commonUri, Transformer transformer, float minGeoX, float minGeoY, float maxGeoX, float maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException {
    	float targetResolution = (maxGeoX - minGeoX) / w;
        GoogleLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);
        
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
    	String size = Long.toString(w) + "x" + Long.toString(h);
    	
        URIUtils.addParamOverride(tileParams, "center", center);
        URIUtils.addParamOverride(tileParams, "size", size);
        URIUtils.addParamOverride(tileParams, "zoom", Integer.toString(resolution.index));
        URI uri = URIUtils.addParams(commonUri, tileParams, OVERRIDE_ALL);

        if(config.signer != null) {
            return config.signURI(uri);
        } else {
            return uri;
        }
    }


    public static void create(List<MapReader> target, RenderingContext context, PJsonObject params) {
        target.add(new GoogleMapTileReader("t", context, params));
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

	@Override
	protected void addCommonQueryParams(Map<String, List<String>> result,
			Transformer transformer, String srs, boolean first) {
        if(config.signer != null) {
            URIUtils.addParamOverride(result, "client", config.signer.clientId());
        }
        URIUtils.addParamOverride(result, "sensor", config.sensor);
        URIUtils.addParamOverride(result, "format", config.format);
        URIUtils.addParamOverride(result, "maptype", config.maptype);
        if(config.language != null) URIUtils.addParamOverride(result, "language", config.language);
        if(config.markers.size() > 0) result.put("markers", config.markers);
        if(config.path != null) URIUtils.addParamOverride(result, "path", config.path);
	}
}