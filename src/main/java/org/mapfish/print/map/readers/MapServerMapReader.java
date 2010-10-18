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

package org.mapfish.print.map.readers;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.StringUtils;
import org.pvalsecc.misc.URIUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapServerMapReader extends HTTPMapReader {
    private final String format;
    protected final List<String> layers = new ArrayList<String>();

    private MapServerMapReader(String layer, RenderingContext context, PJsonObject params) {
        super(context, params);
        layers.add(layer);
        format = params.getString("format");
    }

    protected void renderTiles(TileRenderer formatter, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException {
        //tiling not supported and not really needed (tilecache doesn't support this protocol) for MapServer protocol...
        List<URI> uris = new ArrayList<URI>(1);
        uris.add(commonUri);
        formatter.render(transformer, uris, parallelMapTileLoader, context, opacity, 1, 0, 0,
                transformer.getRotatedBitmapW(), transformer.getRotatedBitmapH());
    }

    protected TileRenderer.Format getFormat() {
        if (format.equals("image/svg+xml")) {
            return TileRenderer.Format.SVG;
        } else if (format.equals("application/x-pdf")) {
            return TileRenderer.Format.PDF;
        } else {
            return TileRenderer.Format.BITMAP;
        }
    }

    protected void addCommonQueryParams(Map<String, List<String>> result, Transformer transformer, String srs, boolean first) {
        final long w;
        final long h;
        if (format.equals("image/svg+xml")) {
            URIUtils.addParamOverride(result, "map_imagetype", "svg");
            w = transformer.getRotatedSvgW();
            h = transformer.getRotatedSvgH();
        } else if (format.equals("application/x-pdf")) {
            URIUtils.addParamOverride(result, "MAP_IMAGETYPE", "pdf");
            w = transformer.getRotatedBitmapW();
            h = transformer.getRotatedBitmapH();
        } else {
            URIUtils.addParamOverride(result, "MAP_IMAGETYPE", "png");
            w = transformer.getRotatedBitmapW();
            h = transformer.getRotatedBitmapH();
        }
        URIUtils.addParamOverride(result, "MODE", "map");
        URIUtils.addParamOverride(result, "LAYERS", StringUtils.join(layers, " "));
        //URIUtils.addParamOverride(result, "SRS", srs);
        URIUtils.addParamOverride(result, "MAP_SIZE", String.format("%d %d", w, h));
        URIUtils.addParamOverride(result, "MAPEXT", String.format("%s %s %s %s", transformer.getRotatedMinGeoX(), transformer.getRotatedMinGeoY(), transformer.getRotatedMaxGeoX(), transformer.getRotatedMaxGeoY()));
        if (!first) {
            URIUtils.addParamOverride(result, "TRANSPARENT", "true");
        }
    }

    protected static void create(List<MapReader> target, RenderingContext context, PJsonObject params) {
        PJsonArray layers = params.getJSONArray("layers");
        for (int i = 0; i < layers.size(); i++) {
            String layer = layers.getString(i);
            target.add(new MapServerMapReader(layer, context, params));
        }
    }

    public boolean testMerge(MapReader other) {
        if (canMerge(other)) {
            MapServerMapReader ms = (MapServerMapReader) other;
            layers.addAll(ms.layers);
            return true;
        }
        return false;
    }

    public boolean canMerge(MapReader other) {
        if (!super.canMerge(other)) {
            return false;
        }

        if (other instanceof MapServerMapReader) {
            MapServerMapReader wms = (MapServerMapReader) other;
            return format.equals(wms.format);
        } else {
            return false;
        }
    }

    public String toString() {
        return StringUtils.join(layers, ", ");
    }
}