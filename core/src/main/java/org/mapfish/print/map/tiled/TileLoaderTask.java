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

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import jsr166y.ForkJoinTask;
import jsr166y.RecursiveTask;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapBounds;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * @author Jesse on 4/3/14.
 */
public final class TileLoaderTask extends RecursiveTask<GridCoverage2D> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TileLoaderTask.class);

    private final MapBounds bounds;
    private final Rectangle paintArea;
    private final double dpi;
    private final TileCacheInformation tiledLayer;
    private final BufferedImage errorImage;

    /**
     * Constructor.
     *
     * @param bounds        the map bounds
     * @param paintArea     the area to paint
     * @param dpi           the DPI to render at
     * @param tileCacheInfo the object used to create the tile requests
     */
    public TileLoaderTask(final MapBounds bounds, final Rectangle paintArea,
                          final double dpi, final TileCacheInformation tileCacheInfo) {
        this.bounds = bounds;
        this.paintArea = paintArea;
        this.dpi = dpi;
        this.tiledLayer = tileCacheInfo;
        final Dimension tileSize = this.tiledLayer.getTileSize();
        this.errorImage = new BufferedImage(tileSize.width, tileSize.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = this.errorImage.createGraphics();
        try {
            // CSOFF:MagicNumber
            graphics.setBackground(new Color(255, 155, 155));
            // CSON:MagicNumber
            graphics.clearRect(0, 0, tileSize.width, tileSize.height);
        } finally {
            graphics.dispose();
        }
    }

    @Override
    protected GridCoverage2D compute() {
        try {
        final ReferencedEnvelope mapGeoBounds = this.bounds.toReferencedEnvelope(this.paintArea, this.dpi);
        final CoordinateReferenceSystem mapProjection = mapGeoBounds.getCoordinateReferenceSystem();
        Dimension tileSizeOnScreen = this.tiledLayer.getTileSize();

        final double mapResolution = this.bounds.getResolution(this.paintArea, this.dpi);
        Coordinate tileSizeInWorld = new Coordinate(tileSizeOnScreen.width * mapResolution, tileSizeOnScreen.height * mapResolution);

        // The minX minY of the first (minY,minY) tile
        Coordinate gridCoverageOrigin = this.tiledLayer.getMinGeoCoordinate(mapGeoBounds, tileSizeInWorld);

        // the offset from the mapGeoBound's minx and miny
        Coordinate coverageOffset = new Coordinate(
                (mapGeoBounds.getMinX() - gridCoverageOrigin.x) / mapResolution,
                (mapGeoBounds.getMinY() - gridCoverageOrigin.y) / mapResolution);

        URI commonUri = this.tiledLayer.createCommonURI();


        int imageWidth = 0;
        int imageHeight = 0;
        double gridCoverageMaxX = gridCoverageOrigin.x;
        double gridCoverageMaxY = gridCoverageOrigin.y;
        List<ForkJoinTask<Tile>> loaderTasks = Lists.newArrayList();
        for (double geoY = gridCoverageOrigin.y; geoY < mapGeoBounds.getMaxY(); geoY += tileSizeInWorld.y) {
            imageWidth = 0;
            gridCoverageMaxX = gridCoverageOrigin.x;
            for (double geoX = gridCoverageOrigin.x; geoX < mapGeoBounds.getMaxX(); geoX += tileSizeInWorld.x) {
                ReferencedEnvelope tileBounds = new ReferencedEnvelope(
                        geoX, geoX + tileSizeInWorld.x, geoY, geoY + tileSizeInWorld.y, mapProjection);
                ClientHttpRequest tileRequest = this.tiledLayer.getTileRequest(commonUri, tileBounds, tileSizeOnScreen);
                if (this.tiledLayer.isVisible(tileBounds)) {
                    final SingleTileLoaderTask task = new SingleTileLoaderTask(tileRequest, this.errorImage, imageWidth, imageHeight);
                    loaderTasks.add(task);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Tile out of bounds: " + tileRequest);
                    }
                    loaderTasks.add(new PlaceHolderImageTask(this.tiledLayer.getMissingTileImage(), imageWidth, imageHeight));
                }
                imageWidth += tileSizeOnScreen.width;  // important to leave this at end of the loop
                gridCoverageMaxX += tileSizeInWorld.x;
            }
            imageHeight += tileSizeOnScreen.height;  // important to leave this at end of the loop
            gridCoverageMaxY += tileSizeInWorld.y;
        }


        BufferedImage coverageImage = this.tiledLayer.createBufferedImage(imageWidth, imageHeight);
        Graphics2D graphics = coverageImage.createGraphics();
        try {
            for (ForkJoinTask<Tile> loaderTask : loaderTasks) {
                Tile tile = loaderTask.invoke();
                if (tile.image != null) {
                    graphics.drawImage(tile.image, tile.originX, tile.originY, null);
                }
            }
        } finally {
            graphics.dispose();
        }

        GridCoverageBuilder coverageBuilder = new GridCoverageBuilder();
        coverageBuilder.setBufferedImage(coverageImage);
        coverageBuilder.setCoordinateReferenceSystem(mapProjection);
        coverageBuilder.setEnvelope(gridCoverageOrigin.x, gridCoverageOrigin.y, gridCoverageMaxX, gridCoverageMaxY);

        // Set some metadata on the coverage regarding the sample sizes.  This can be used to style coverage.
        // TODO should be more sophisticated in future regarding the different subclasses of SampleModels
        final SampleModel sampleModel = coverageImage.getSampleModel();
        for (int i = 0; i < sampleModel.getNumBands(); i++) {
            coverageBuilder.setSampleRange(0, 2 ^ sampleModel.getSampleSize(i));
        }

        return coverageBuilder.getGridCoverage2D();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class SingleTileLoaderTask extends RecursiveTask<Tile> {

        private final ClientHttpRequest tileRequest;
        private final int tileOriginX;
        private final int tileOriginY;
        private final BufferedImage errorImage;

        public SingleTileLoaderTask(final ClientHttpRequest tileRequest, final BufferedImage errorImage, final int tileOriginX,
                                    final int tileOriginY) {
            this.tileRequest = tileRequest;
            this.tileOriginX = tileOriginX;
            this.tileOriginY = tileOriginY;
            this.errorImage = errorImage;
        }

        @Override
        protected Tile compute() {
            ClientHttpResponse response = null;
            try {
                response = this.tileRequest.execute();
                final HttpStatus statusCode = response.getStatusCode();
                if (statusCode != HttpStatus.OK) {
                    LOGGER.error("Error making tile request: " + this.tileRequest.getURI() + "\n\tStatus: " + statusCode +
                                 "\n\tMessage: " + response.getStatusText());
                    return new Tile(this.errorImage, this.tileOriginX, this.tileOriginY);
                }

                BufferedImage image = ImageIO.read(response.getBody());

                return new Tile(image, this.tileOriginX, this.tileOriginY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
    }

    private static class PlaceHolderImageTask extends RecursiveTask<Tile> {

        private final BufferedImage placeholderImage;
        private final int tileOriginX;
        private final int tileOriginY;

        public PlaceHolderImageTask(final BufferedImage placeholderImage, final int tileOriginX, final int tileOriginY) {
            this.placeholderImage = placeholderImage;
            this.tileOriginX = tileOriginX;
            this.tileOriginY = tileOriginY;
        }

        @Override
        protected Tile compute() {
            return new Tile(this.placeholderImage, this.tileOriginX, this.tileOriginY);
        }
    }

    private static final class Tile {
        /**
         * The tile image.
         */
        private final BufferedImage image;
        /**
         * The x coordinate to start drawing the tile.  It should be the minx of the tile.
         */
        private final int originX;
        /**
         * The y coordinate to start drawing the tile.  It should be the miny of the tile.
         */
        private final int originY;

        private Tile(final BufferedImage image, final int originX, final int originY) {
            this.image = image;
            this.originX = originX;
            this.originY = originY;
        }
    }
}
