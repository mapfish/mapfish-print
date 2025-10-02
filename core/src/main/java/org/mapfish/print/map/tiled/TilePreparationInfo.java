package org.mapfish.print.map.tiled;

import java.util.List;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.http.client.ClientHttpRequest;

/**
 * Tile Preparation Task Result.
 *
 * @param singleTiles tiles
 * @param imageWidth image width
 * @param imageHeight image height
 * @param gridCoverageOrigin grid coverage origin
 * @param gridCoverageMaxX grid coverage max x
 * @param gridCoverageMaxY grid coverage max y
 * @param mapProjection map projection
 */
public record TilePreparationInfo(
    List<SingleTilePreparationInfo> singleTiles,
    int imageWidth,
    int imageHeight,
    Coordinate gridCoverageOrigin,
    double gridCoverageMaxX,
    double gridCoverageMaxY,
    CoordinateReferenceSystem mapProjection) {

  /**
   * Information per tile (x, y and request).
   *
   * @param tileIndexX tile index x
   * @param tileIndexY tile index y
   * @param tileRequest tile request
   */
  public record SingleTilePreparationInfo(
      int tileIndexX, int tileIndexY, ClientHttpRequest tileRequest) {}
}
