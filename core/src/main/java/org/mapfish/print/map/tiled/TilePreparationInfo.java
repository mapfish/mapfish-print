package org.mapfish.print.map.tiled;

import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.client.ClientHttpRequest;

import java.util.List;

/**
 * Tile Preparation Task Result.
 */
public class TilePreparationInfo {

    private final List<SingleTilePreparationInfo> singleTiles;
    private final int imageWidth;
    private final int imageHeight;
    private final Coordinate gridCoverageOrigin;
    private final double gridCoverageMaxX;
    private final double gridCoverageMaxY;
    private final CoordinateReferenceSystem mapProjection;

    /**
     * Constructor.
     *
     * @param singleTiles tiles
     * @param imageWidth image width
     * @param imageHeight image height
     * @param gridCoverageOrigin grid coverage origin
     * @param gridCoverageMaxX grid coverage max x
     * @param gridCoverageMaxY grid coverage max y
     * @param mapProjection map projection
     */
    public TilePreparationInfo(
            final List<SingleTilePreparationInfo> singleTiles, final int imageWidth, final int imageHeight,
            final Coordinate gridCoverageOrigin, final double gridCoverageMaxX, final double gridCoverageMaxY,
            final CoordinateReferenceSystem mapProjection) {
        this.singleTiles = singleTiles;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.gridCoverageOrigin = gridCoverageOrigin;
        this.gridCoverageMaxX = gridCoverageMaxX;
        this.gridCoverageMaxY = gridCoverageMaxY;
        this.mapProjection = mapProjection;
    }

    public final List<SingleTilePreparationInfo> getSingleTiles() {
        return this.singleTiles;
    }

    public final int getImageWidth() {
        return this.imageWidth;
    }

    public final int getImageHeight() {
        return this.imageHeight;
    }

    public final double getGridCoverageMaxX() {
        return this.gridCoverageMaxX;
    }

    public final double getGridCoverageMaxY() {
        return this.gridCoverageMaxY;
    }

    public final Coordinate getGridCoverageOrigin() {
        return this.gridCoverageOrigin;
    }

    public final CoordinateReferenceSystem getMapProjection() {
        return this.mapProjection;
    }

    /**
     * Information per tile (x, y and request).
     */
    public static class SingleTilePreparationInfo {
        private final int tileIndexX;
        private final int tileIndexY;
        private ClientHttpRequest tileRequest;

        /**
         * Constructor.
         *
         * @param tileIndexX tile index x
         * @param tileIndexY tile index y
         * @param tileRequest tile request
         */
        public SingleTilePreparationInfo(
                final int tileIndexX, final int tileIndexY, final ClientHttpRequest tileRequest) {
            super();
            this.tileIndexX = tileIndexX;
            this.tileIndexY = tileIndexY;
            this.tileRequest = tileRequest;
        }

        public final int getTileIndexX() {
            return this.tileIndexX;
        }

        public final int getTileIndexY() {
            return this.tileIndexY;
        }

        public final ClientHttpRequest getTileRequest() {
            return this.tileRequest;
        }
    }
}
