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
import org.mapfish.print.utils.PJsonObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public abstract class TileableMapReader extends HTTPMapReader {

    protected TileCacheLayerInfo tileCacheLayerInfo;

    protected TileableMapReader(RenderingContext context, PJsonObject params) {
        super(context, params);
    }

    protected void renderTiles(TileRenderer formater, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException, URISyntaxException {
        final List<URI> urls = new ArrayList<URI>(1);
        final float offsetX;
        final float offsetY;
        final long bitmapTileW;
        final long bitmapTileH;
        int nbTilesW = 0;

        float minGeoX = transformer.getRotatedMinGeoX();
        float minGeoY = transformer.getRotatedMinGeoY();
        float maxGeoX = transformer.getRotatedMaxGeoX();
        float maxGeoY = transformer.getRotatedMaxGeoY();

        if (tileCacheLayerInfo != null) {
            //tiled
            transformer = fixTiledTransformer(transformer);
            bitmapTileW = tileCacheLayerInfo.getWidth();
            bitmapTileH = tileCacheLayerInfo.getHeight();
            final float tileGeoWidth = transformer.getResolution() * bitmapTileW;
            final float tileGeoHeight = transformer.getResolution() * bitmapTileH;


            // TODO I would like to do this sort of thing by extension points for plugins
            
            // the tileMinGeoSize is not calculated the same way in TileCache
            // and KaMap, so they are treated differently here.
            final float tileMinGeoX;
            final float tileMinGeoY;
            if (this instanceof TileCacheMapReader) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("TileCacheMapReader min geo x and y calculation used");
                }
                tileMinGeoX = (float) (Math.floor((minGeoX - tileCacheLayerInfo.getMinX()) / tileGeoWidth) * tileGeoWidth) + tileCacheLayerInfo.getMinX();
                tileMinGeoY = (float) (Math.floor((minGeoY - tileCacheLayerInfo.getMinY()) / tileGeoHeight) * tileGeoHeight) + tileCacheLayerInfo.getMinY();
            } else if (this instanceof KaMapCacheMapReader || this instanceof KaMapMapReader) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Kamap min geo x and y calculation used");
                }
                tileMinGeoX = (float) (Math.floor((minGeoX) / tileGeoWidth) * tileGeoWidth);
                tileMinGeoY = (float) (Math.floor((minGeoY) / tileGeoHeight) * tileGeoHeight);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Default min geo x and y calculation used");
                }
                tileMinGeoX = (float) (Math.floor((minGeoX - tileCacheLayerInfo.getMinX()) / tileGeoWidth) * tileGeoWidth) + tileCacheLayerInfo.getMinX();
                tileMinGeoY = (float) (Math.floor((minGeoY - tileCacheLayerInfo.getMinY()) / tileGeoHeight) * tileGeoHeight) + tileCacheLayerInfo.getMinY();
            }

            offsetX = (minGeoX - tileMinGeoX) / transformer.getResolution();
            offsetY = (minGeoY - tileMinGeoY) / transformer.getResolution();
            for (float geoY = tileMinGeoY; geoY < maxGeoY; geoY += tileGeoHeight) {
                nbTilesW = 0;
                for (float geoX = tileMinGeoX; geoX < maxGeoX; geoX += tileGeoWidth) {
                    nbTilesW++;
                    if (tileCacheLayerInfo.isVisible(geoX, geoY, geoX + tileGeoWidth, geoY + tileGeoHeight)) {
                        urls.add(getTileUri(commonUri, transformer, geoX, geoY, geoX + tileGeoWidth, geoY + tileGeoHeight, bitmapTileW, bitmapTileH));
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Tile out of bounds: " + getTileUri(commonUri, transformer, geoX, geoY, geoX + tileGeoWidth, geoY + tileGeoHeight, bitmapTileW, bitmapTileH));
                        }
                        urls.add(null);
                    }
                }
            }

        } else {
            //single tile
            nbTilesW = 1;
            offsetX = 0;
            offsetY = 0;
            bitmapTileW = transformer.getRotatedBitmapW();
            bitmapTileH = transformer.getRotatedBitmapH();
            urls.add(getTileUri(commonUri, transformer, minGeoX, minGeoY, maxGeoX, maxGeoY, bitmapTileW, bitmapTileH));
        }
        formater.render(transformer, urls, parallelMapTileLoader, context, opacity, nbTilesW, offsetX, offsetY, bitmapTileW, bitmapTileH);
    }

    /**
     * fix the resolution to something compatible with the resolutions available in tilecache.
     */
    private Transformer fixTiledTransformer(Transformer transformer) {
        float targetResolution = transformer.getGeoW() / transformer.getStraightBitmapW();
        TileCacheLayerInfo.ResolutionInfo resolution = tileCacheLayerInfo.getNearestResolution(targetResolution);
        transformer = transformer.clone();
        transformer.setResolution(resolution.value);
        return transformer;
    }

    /**
     * Adds the query parameters for the given tile.
     */
    protected abstract URI getTileUri(URI commonUri, Transformer transformer, float minGeoX, float minGeoY, float maxGeoX, float maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException;
}
