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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.TileRenderer;
import org.mapfish.print.utils.PJsonObject;

public abstract class TileableMapReader extends HTTPMapReader {

    protected TileCacheLayerInfo tileCacheLayerInfo;

    protected TileableMapReader(RenderingContext context, PJsonObject params) {
        super(context, params);
    }

    protected void renderTiles(TileRenderer formatter, Transformer transformer, URI commonUri, ParallelMapTileLoader parallelMapTileLoader) throws IOException, URISyntaxException {
        final List<URI> urls = new ArrayList<URI>(1);
        final double offsetX;
        final double offsetY;
        final long bitmapTileW;
        final long bitmapTileH;
        int nbTilesW = 0;

        double minGeoX = transformer.getRotatedMinGeoX();
        double minGeoY = transformer.getRotatedMinGeoY();
        double maxGeoX = transformer.getRotatedMaxGeoX();
        double maxGeoY = transformer.getRotatedMaxGeoY();

        if (tileCacheLayerInfo != null) {
            try {
            //tiled
            transformer = fixTiledTransformer(transformer);
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(TileableMapReader.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (transformer == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Resolution out of bounds.");
                }
                urls.add(null);
            }

            bitmapTileW = tileCacheLayerInfo.getWidth();
            bitmapTileH = tileCacheLayerInfo.getHeight();
            final double tileGeoWidth = transformer.getResolution() * bitmapTileW;
            final double tileGeoHeight = transformer.getResolution() * bitmapTileH;


            // TODO I would like to do this sort of thing by extension points for plugins

            // the tileMinGeoSize is not calculated the same way in TileCache
            // and KaMap, so they are treated differently here.
            final float tileMinGeoX;
            final float tileMinGeoY;
            if (this instanceof KaMapCacheMapReader || this instanceof KaMapMapReader) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Kamap min geo x and y calculation used");
                }
                tileMinGeoX = (float) (Math.floor((minGeoX) / tileGeoWidth) * tileGeoWidth);
                tileMinGeoY = (float) (Math.floor((minGeoY) / tileGeoHeight) * tileGeoHeight);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Default min geo x and y calculation used");
                }
                tileMinGeoX = (float) (tileCacheLayerInfo.getOriginX() + (Math.floor(
                    (minGeoX - tileCacheLayerInfo.getOriginX()) / tileGeoWidth
                ) * tileGeoWidth));
                tileMinGeoY = (float) (tileCacheLayerInfo.getOriginY() + (Math.floor(
                    (minGeoY - tileCacheLayerInfo.getOriginY()) / tileGeoHeight
                ) * tileGeoHeight));
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
        formatter.render(transformer, urls, parallelMapTileLoader, context, opacity, nbTilesW, offsetX, offsetY, bitmapTileW, bitmapTileH);
    }

    /**
     * fix the resolution to something compatible with the resolutions available in tilecache.
     */
    private Transformer fixTiledTransformer(Transformer transformer) throws CloneNotSupportedException {
        double resolution;

        // if clientResolution is passed from client use it explicitly if available otherwise calculate nearest resolution
        if (this.context.getCurrentPageParams().has("clientResolution")) {
            float clientResolution = this.context.getCurrentPageParams().getFloat("clientResolution");
            boolean hasServerResolution = false;
            for (double serverResolution : this.tileCacheLayerInfo.getResolutions()) {
                if (serverResolution == clientResolution) {
                    hasServerResolution = true;
                }
            }
            if (!hasServerResolution) {
                return null;
            }
            else {
                resolution = clientResolution;
            }
        }
        else {
            double targetResolution = transformer.getGeoW() / transformer.getStraightBitmapW();
            TileCacheLayerInfo.ResolutionInfo resolutionInfo = tileCacheLayerInfo.getNearestResolution(targetResolution);
            resolution = resolutionInfo.value;
        }

        transformer = transformer.clone();
        transformer.setResolution(resolution);
        return transformer;
    }

    /**
     * Adds the query parameters for the given tile.
     */
    protected abstract URI getTileUri(URI commonUri, Transformer transformer, double minGeoX, double minGeoY, double maxGeoX, double maxGeoY, long w, long h) throws URISyntaxException, UnsupportedEncodingException;
}
