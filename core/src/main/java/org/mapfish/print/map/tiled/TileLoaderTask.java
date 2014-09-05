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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import jsr166y.RecursiveTask;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
    private final MapfishMapContext transformer;
    private final TileCacheInformation tiledLayer;
    private final BufferedImage errorImage;
    private final MfClientHttpRequestFactory httpRequestFactory;
    private Optional<Geometry> cachedRotatedMapBounds = null;

    /**
     * Constructor.
     *  @param httpRequestFactory the factory to use for making http requests
     * @param dpi                the DPI to render at
     * @param transformer        a transformer for making calculations
     * @param tileCacheInfo      the object used to create the tile requests
     */
    public TileLoaderTask(final MfClientHttpRequestFactory httpRequestFactory,
                          final double dpi,
                          final MapfishMapContext transformer,
                          final TileCacheInformation tileCacheInfo) {
        this.bounds = transformer.getBounds();
        this.paintArea = new Rectangle(transformer.getMapSize());
        this.dpi = dpi;
        this.httpRequestFactory = httpRequestFactory;
        this.transformer = transformer;
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

            final double layerDpi = (this.tiledLayer.getLayerDpi() != null) ? this.tiledLayer.getLayerDpi() : this.dpi;
            final double layerResolution = this.tiledLayer.getScale().toResolution(mapProjection, layerDpi);
            Coordinate tileSizeInWorld = new Coordinate(tileSizeOnScreen.width * layerResolution,
                    tileSizeOnScreen.height * layerResolution);

            // The minX minY of the first (minY,minY) tile
            Coordinate gridCoverageOrigin = this.tiledLayer.getMinGeoCoordinate(mapGeoBounds, tileSizeInWorld);

            final String commonUrl = this.tiledLayer.createCommonUrl();

            ReferencedEnvelope tileCacheBounds = this.tiledLayer.getTileCacheBounds();
            final double resolution = this.tiledLayer.getScale().toResolution(this.bounds.getProjection(), layerDpi);
            double rowFactor = 1 / (resolution * tileSizeOnScreen.height);
            double columnFactor = 1 / (resolution * tileSizeOnScreen.width);

            int imageWidth = 0;
            int imageHeight = 0;
            int xIndex;
            int yIndex = (int) Math.floor((mapGeoBounds.getMaxY() - gridCoverageOrigin.y) / tileSizeInWorld.y) + 1;

            double gridCoverageMaxX = gridCoverageOrigin.x;
            double gridCoverageMaxY = gridCoverageOrigin.y;
            List<TileTask> loaderTasks = Lists.newArrayList();

            for (double geoY = gridCoverageOrigin.y; geoY < mapGeoBounds.getMaxY(); geoY += tileSizeInWorld.y) {
                yIndex--;
                imageHeight += tileSizeOnScreen.height;
                imageWidth = 0;
                xIndex = -1;

                gridCoverageMaxX = gridCoverageOrigin.x;
                gridCoverageMaxY += tileSizeInWorld.y;
                for (double geoX = gridCoverageOrigin.x; geoX < mapGeoBounds.getMaxX(); geoX += tileSizeInWorld.x) {
                    xIndex++;
                    imageWidth += tileSizeOnScreen.width;
                    gridCoverageMaxX += tileSizeInWorld.x;

                    ReferencedEnvelope tileBounds = new ReferencedEnvelope(
                            geoX, geoX + tileSizeInWorld.x, geoY, geoY + tileSizeInWorld.y, mapProjection);

                    int row = (int) Math.round((tileCacheBounds.getMaxY() - tileBounds.getMaxY()) * rowFactor);
                    int column = (int) Math.round((tileBounds.getMinX() - tileCacheBounds.getMinX()) * columnFactor);

                    ClientHttpRequest tileRequest = this.tiledLayer.getTileRequest(this.httpRequestFactory, commonUrl, tileBounds,
                            tileSizeOnScreen, column, row);

                    if (isInTileCacheBounds(tileCacheBounds, tileBounds)) {
                        if (isTileVisible(tileBounds)) {
                            final SingleTileLoaderTask task = new SingleTileLoaderTask(tileRequest, this.errorImage, xIndex, yIndex);
                            loaderTasks.add(task);
                        }
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Tile out of bounds: " + tileRequest);
                        }
                        loaderTasks.add(new PlaceHolderImageTask(this.tiledLayer.getMissingTileImage(), xIndex, yIndex));
                    }
                }
            }

            BufferedImage coverageImage = this.tiledLayer.createBufferedImage(imageWidth, imageHeight);
            Graphics2D graphics = coverageImage.createGraphics();
            try {
                for (TileTask loaderTask : loaderTasks) {
                    Tile tile = loaderTask.invoke();
                    if (tile.image != null) {
                        graphics.drawImage(tile.image,
                                tile.xIndex * tileSizeOnScreen.width, tile.yIndex * tileSizeOnScreen.height, null);
                    }
                }
            } finally {
                graphics.dispose();
            }

            GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
            GeneralEnvelope gridEnvelope = new GeneralEnvelope(mapProjection);
            gridEnvelope.setEnvelope(gridCoverageOrigin.x, gridCoverageOrigin.y, gridCoverageMaxX, gridCoverageMaxY);
            return factory.create(commonUrl.toString(), coverageImage, gridEnvelope, null, null, null);
        } catch (Exception e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    private boolean isInTileCacheBounds(final ReferencedEnvelope tileCacheBounds, final ReferencedEnvelope tilesBounds) {
        final double boundsMinX = tilesBounds.getMinX();
        final double boundsMinY = tilesBounds.getMinY();
        return boundsMinX >= tileCacheBounds.getMinX() && boundsMinX <= tileCacheBounds.getMaxX()
               && boundsMinY >= tileCacheBounds.getMinY() && boundsMinY <= tileCacheBounds.getMaxY();
        //we don't use maxX and maxY since tilecache doesn't seems to care about those...
    }

    /**
     * When using a map rotation, there might be tiles that are outside the
     * rotated map area. To avoid to load these tiles, this method checks
     * if a tile is really required to draw the map.
     */
    private boolean isTileVisible(final ReferencedEnvelope tileBounds) {
        if (this.transformer.getRotation() == 0.0) {
            return true;
        }

        final GeometryFactory gfac = new GeometryFactory();
        final Optional<Geometry> rotatedMapBounds = getRotatedMapBounds(gfac);

        if (rotatedMapBounds.isPresent()) {
            return rotatedMapBounds.get().intersects(gfac.toGeometry(tileBounds));
        } else {
            // in case of an error, we simply load the tile
            return true;
        }
    }

    private Optional<Geometry> getRotatedMapBounds(final GeometryFactory gfac) {
        if (this.cachedRotatedMapBounds != null) {
            return this.cachedRotatedMapBounds;
        }

        // get the bounds for the unrotated map area
        final ReferencedEnvelope mapBounds = this.transformer.getBounds().toReferencedEnvelope(
                new Rectangle(this.transformer.getMapSize()), this.dpi);

        // then rotate the geometry around its center
        final Coordinate center = mapBounds.centre();
        final AffineTransform affineTransform = AffineTransform.getRotateInstance(
                this.transformer.getRotation(), center.x, center.y);
        final MathTransform mathTransform = new AffineTransform2D(affineTransform);

        try {
            final Geometry rotatedBounds = JTS.transform(gfac.toGeometry(mapBounds), mathTransform);
            this.cachedRotatedMapBounds = Optional.of(rotatedBounds);
        } catch (TransformException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to rotate map bounds: " + mapBounds.toString(), e);
            }
            this.cachedRotatedMapBounds = Optional.absent();
        }

        return this.cachedRotatedMapBounds;
    }

    private abstract static class TileTask extends RecursiveTask<Tile> {
        private final int tileIndexX;
        private final int tileIndexY;

        public TileTask(final int tileIndexX, final int tileIndexY) {
            this.tileIndexX = tileIndexX;
            this.tileIndexY = tileIndexY;
        }

        public int getTileIndexX() {
            return this.tileIndexX;
        }

        public int getTileIndexY() {
            return this.tileIndexY;
        }
    }

    private static final class SingleTileLoaderTask extends TileTask {

        private final ClientHttpRequest tileRequest;
        private final BufferedImage errorImage;

        public SingleTileLoaderTask(final ClientHttpRequest tileRequest, final BufferedImage errorImage, final int tileIndexX,
                                    final int tileIndexY) {
            super(tileIndexX, tileIndexY);
            this.tileRequest = tileRequest;
            this.errorImage = errorImage;
        }

        @Override
        protected Tile compute() {
            ClientHttpResponse response = null;
            try {
                LOGGER.debug("\n\t" + this.tileRequest.getMethod() + " -- " + this.tileRequest.getURI());
                response = this.tileRequest.execute();
                final HttpStatus statusCode = response.getStatusCode();
                if (statusCode != HttpStatus.OK) {
                    LOGGER.error("Error making tile request: " + this.tileRequest.getURI() + "\n\tStatus: " + statusCode +
                                 "\n\tMessage: " + response.getStatusText());
                    return new Tile(this.errorImage, getTileIndexX(), getTileIndexY());
                }

                BufferedImage image = ImageIO.read(response.getBody());
                if (image == null) {
                    LOGGER.warn("The URL: " + this.tileRequest.getURI() + " is an image format that can be decoded");
                    image = this.errorImage;
                }

                return new Tile(image, getTileIndexX(), getTileIndexY());
            } catch (IOException e) {
                throw ExceptionUtils.getRuntimeException(e);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }
    }

    private static class PlaceHolderImageTask extends TileTask {

        private final BufferedImage placeholderImage;

        public PlaceHolderImageTask(final BufferedImage placeholderImage, final int tileOriginX, final int tileOriginY) {
            super(tileOriginX, tileOriginY);
            this.placeholderImage = placeholderImage;
        }

        @Override
        protected Tile compute() {
            return new Tile(this.placeholderImage, getTileIndexX(), getTileIndexY());
        }
    }

    private static final class Tile {
        /**
         * The tile image.
         */
        private final BufferedImage image;
        /**
         * The x index of the image.  the x coordinate to draw this tile is xIndex * tileSizeX
         */
        private final int xIndex;
        /**
         * The y index of the image.  the y coordinate to draw this tile is yIndex * tileSizeY
         */
        private final int yIndex;

        private Tile(final BufferedImage image, final int xIndex, final int yIndex) {
            this.image = image;
            this.xIndex = xIndex;
            this.yIndex = yIndex;
        }
    }
}
