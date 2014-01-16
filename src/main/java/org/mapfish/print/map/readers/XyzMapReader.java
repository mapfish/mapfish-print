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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

/**
 * Support the tile layout z/x/y.<extension>.
 */
public class XyzMapReader extends TileableMapReader {
    public static class Factory implements MapReaderFactory {
        @Override
        public List<? extends MapReader> create(String type, RenderingContext context,
                                                PJsonObject params) {
            return Collections.singletonList(new XyzMapReader("t", context, params));
        }
    }

    protected final String layer;
    protected final String path_format;

    protected XyzMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        this.layer = layer;
        PJsonArray maxExtent = params.getJSONArray("maxExtent");
        PJsonArray tileSize = params.getJSONArray("tileSize");
        PJsonArray tileOrigin = params.optJSONArray("tileOrigin");
        String tileOriginCorner = params.optString("tileOriginCorner", "bl");
        final float tileOriginX;
        final float tileOriginY;
        if (tileOrigin == null) {
            tileOriginX = maxExtent.getFloat(0);
            tileOriginY = maxExtent.getFloat(tileOriginCorner.charAt(0) == 't' ? 3 : 1);
        } else {
            tileOriginX = tileOrigin.getFloat(0);
            tileOriginY = tileOrigin.getFloat(1);
        }

        path_format = params.optString("path_format", null);
        tileCacheLayerInfo = new XyzLayerInfo(params.getJSONArray("resolutions"), tileSize.getInt(0), tileSize.getInt(1), maxExtent.getFloat(0), maxExtent.getFloat(1), maxExtent.getFloat(2), maxExtent.getFloat(3),
                params.getString("extension"), tileOriginX, tileOriginY);
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
        XyzLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);

        int tileX = (int) Math.round((minGeoX - tileCacheLayerInfo.getMinX()) / (resolution.value * w));
        int tileY = (int) Math.round((tileCacheLayerInfo.getMaxY() - minGeoY) / (resolution.value * h));

        StringBuilder path = new StringBuilder();
        if (!commonUri.getPath().endsWith("/")) {
            path.append('/');
        }

        if (this.path_format == null) {
            path.append(String.format("%02d", resolution.index));
            path.append('/').append(tileX);
            path.append('/').append(tileY - 1);
            path.append('.').append(tileCacheLayerInfo.getExtension());
        } else {
            if (this.path_format.startsWith("/")) {
                path.append(this.path_format.substring(1));
            } else {
                path.append(this.path_format);
            }

             url_regex_replace("z", path, resolution.index);
             url_regex_replace("x", path, new Integer(tileX));
             url_regex_replace("y", path, new Integer(tileY - 1));
             url_regex_replace("extension", path, tileCacheLayerInfo.getExtension());
        }

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

    private void url_regex_replace(String needle, StringBuilder haystack, Object replaceValue) {
        Pattern pattern = Pattern.compile("\\$\\{("+needle+"+)\\}");
        Matcher matcher = pattern.matcher(haystack);
        while (matcher.find()) {
            int length = 1;
            if (matcher.groupCount() > 0) {
                length = matcher.group(1).length();
            }
            String value = "";
            if (needle.equals("extension")) {
                value = (String) replaceValue;
            } else {
                value = String.format("%0" + length + "d", replaceValue);
            }
            haystack.replace(matcher.start(), matcher.end(), value);

            matcher = pattern.matcher(haystack);
        }
    }
}
