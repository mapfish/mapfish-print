/*
 * Copyright (C) 2014  Camptocamp
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

package org.mapfish.print.map.tiled;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapBounds;
import org.springframework.http.client.ClientHttpRequest;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Encapsulates the information needed to create tile requests for a particular map bounds and display.
 *
 * @author Jesse on 4/3/14.
 */
public abstract class TileCacheInformation {

    //CSOFF:VisibilityModifier
    /**
     * the map bounds.
     */
    protected final MapBounds bounds;
    /**
     * the area to paint.
     */
    protected final Rectangle paintArea;
    /**
     * the DPI to render at.
     */
    protected final double dpi;

    private final AbstractTiledLayerParams params;

    //CSON:VisibilityModifier

    /**
     * Constructor.
     *
     * @param bounds    the map bounds
     * @param paintArea the area to paint
     * @param dpi       the DPI to render at
     * @param params    the params with the data for creating the layer.
     */
    public TileCacheInformation(final MapBounds bounds, final Rectangle paintArea, final double dpi,
                                final AbstractTiledLayerParams params) {
        this.bounds = bounds;
        this.paintArea = paintArea;
        this.dpi = dpi;
        this.params = params;
    }

    /**
     * Obtain the image tile size of the tiles that will be loaded from the server.
     */
    public abstract Dimension getTileSize();

    /**
     * Calculate the minx and miny coordinate of the tile that is the minx and miny tile.  It is the starting point of counting
     * tiles to render.
     * <p/>
     * This equates to the minX and minY of the GridCoverage as well.
     *
     * @param envelope    the area that will be displayed.
     * @param geoTileSize the size of each time in world space.
     */
    // CSOFF:DesignForExtension
    @Nonnull
    public Coordinate getMinGeoCoordinate(final ReferencedEnvelope envelope, final Coordinate geoTileSize) {
        // CSON:DesignForExtension
        final ReferencedEnvelope tileCacheBounds = getTileCacheBounds();
        final double tileCacheMinX = tileCacheBounds.getMinX();
        double minGeoX = envelope.getMinX();
        double minGeoY = envelope.getMinY();
        double tileMinGeoX = (tileCacheMinX + (Math.floor((minGeoX - tileCacheMinX) / geoTileSize.x) * geoTileSize.x));
        final double tileCacheMinY = tileCacheBounds.getMinY();
        double tileMinGeoY = (tileCacheMinY + (Math.floor((minGeoY - tileCacheMinY) / geoTileSize.y) * geoTileSize.y));

        return new Coordinate(tileMinGeoX, tileMinGeoY);
    }

    /**
     * Return true if there is a tile to return at these bounds.  This should check the extents of the tile cache and return true
     * if there is a tile there.
     *
     * @param tileBounds the geographic bounds of the tile.
     */
    // CSOFF:DesignForExtension
    public boolean isVisible(final ReferencedEnvelope tileBounds) {
        // CSON:DesignForExtension
        final double boundsMinX = tileBounds.getMinX();
        final double boundsMinY = tileBounds.getMinY();
        ReferencedEnvelope tileCacheBounds = getTileCacheBounds();
        return boundsMinX >= tileCacheBounds.getMinX() && boundsMinX <= tileCacheBounds.getMaxX()
               && boundsMinY >= tileCacheBounds.getMinY() && boundsMinY <= tileCacheBounds.getMaxY();
        //we don't use maxX and maxY since tilecache doesn't seems to care about those...
    }

    /**
     * Return the full bounds of the tileCache.
     */
    @Nonnull
    protected abstract ReferencedEnvelope getTileCacheBounds();

    /**
     * Create a buffered image with the correct image bands etc... for the tiles being loaded.
     *
     * @param imageWidth  width of the image to create
     * @param imageHeight height of the image to create.
     */
    // CSOFF:DesignForExtension
    @Nonnull
    public BufferedImage createBufferedImage(final int imageWidth, final int imageHeight) {
        // CSON:DesignForExtension
        return new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR);
    }

    /**
     * Create the http request for loading the image at the indicated area and the indicated size.
     *
     * @param commonURI        the uri that is common to all tiles.  See {@link #createCommonURI()}
     * @param tileBounds       the bounds of the image in world coordinates
     * @param tileSizeOnScreen the size of the tile on the screen or on the image.
     */
    @Nonnull
    public abstract ClientHttpRequest getTileRequest(URI commonURI, ReferencedEnvelope tileBounds, Dimension tileSizeOnScreen) throws IOException, URISyntaxException;

    /**
     * Return the image to draw in place of a tile that is missing.
     * <p/>
     * If this method returns null nothing will be drawn at the location of the missing image.
     */
    @Nullable
    public abstract BufferedImage getMissingTileImage();

    /**
     * Create a URI that is common to all tiles for this layer.  It may have placeholder like ({matrixId}) if the layer desires.
     * That is up to the layer implementation because the layer is responsible for taking the commonURI and transforming it to
     * a final tile URI.
     */
    // CSOFF:DesignForExtension
    protected URI createCommonURI() throws URISyntaxException, UnsupportedEncodingException {
        // CSOFF:DesignForExtension
        Multimap<String, String> queryParams = HashMultimap.create();

        queryParams.putAll(this.params.getCustomParams());
        queryParams.putAll(this.params.getMergeableParams());

        addCommonQueryParams(queryParams);
        final URI baseUri = this.params.getBaseUri();
        return URIUtils.addParams(baseUri, queryParams, URIUtils.getParameters(baseUri).keySet());
    }

    /**
     * Adds the query parameters common to every tile.
     *
     * @param result       the query params added because of customParams or mergeableQueryParams.
     * @param isFirstLayer if true then this layer is the first (bottom) layer on the map.
     */
    protected abstract void addCommonQueryParams(Multimap<String, String> result);

}
