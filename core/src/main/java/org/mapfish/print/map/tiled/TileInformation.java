package org.mapfish.print.map.tiled;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;

/**
 * Encapsulates the information needed to create tile requests for a particular map bounds and
 * display.
 *
 * @param <T> Type of the params associated to this Tile.
 */
public abstract class TileInformation<T extends AbstractTiledLayerParams> {

  /** the map bounds. */
  protected final MapBounds bounds;

  /** the area to paint. */
  protected final Rectangle paintArea;

  /** the DPI to render at. */
  protected final double dpi;

  private final T params;

  /**
   * Constructor.
   *
   * @param bounds the map bounds
   * @param paintArea the area to paint
   * @param dpi the DPI to render at
   * @param params the params with the data for creating the layer.
   */
  protected TileInformation(
      final MapBounds bounds, final Rectangle paintArea, final double dpi, final T params) {
    this.bounds = bounds;
    this.paintArea = paintArea;
    this.dpi = dpi;
    this.params = params;
  }

  /**
   * Create the http request for loading the image at the indicated area and the indicated size.
   *
   * @param httpRequestFactory the factory to use for making http requests
   * @param commonUrl the uri that is common to all tiles. See {@link #createCommonUrl()}
   * @param tileBounds the bounds of the image in world coordinates
   * @param tileSizeOnScreen the size of the tile on the screen or on the image.
   * @param column the column index of the tile from the origin of the tile.
   * @param row the row index of the tile from the origin of the tile.
   */
  @Nonnull
  public abstract ClientHttpRequest getTileRequest(
      MfClientHttpRequestFactory httpRequestFactory,
      String commonUrl,
      ReferencedEnvelope tileBounds,
      Dimension tileSizeOnScreen,
      int column,
      int row)
      throws Exception;

  /**
   * Get the resolution that the layer uses for its calculations. The map isn't always at a
   * resolution that a tiled layer supports so a scale is chosen for the layer that is close to the
   * map scale. This method returns the layer's scale.
   *
   * <p>This is used for calculating the bounds of tiles, the size number and indices of the tiles
   * to be returned.
   */
  public abstract double getResolution();

  /**
   * Get the DPI of the layer's images. The server renders at a certain DPI that may or may not be
   * the same DPI that the map requires. Depending on the server and the protocol mapfish print
   * might be able to request a certain DPI. But since that might not be the case, then the layer
   * must be able to report the correct DPI.
   */
  public abstract Double getLayerDpi();

  /** Obtain the image tile size of the tiles that will be loaded from the server. */
  public abstract Dimension getTileSize();

  /** Obtain the buffer width for meta tiling. */
  public int getTileBufferWidth() {
    return 0;
  }

  /** Obtain the buffer height for meta tiling. */
  public int getTileBufferHeight() {
    return 0;
  }

  /** Return the full bounds of the tile. */
  @Nonnull
  protected abstract ReferencedEnvelope getTileBounds();

  /**
   * Calculate the minx and miny coordinate of the tile that is the minx and miny tile. It is the
   * starting point of counting tiles to render.
   *
   * <p>This equates to the minX and minY of the GridCoverage as well.
   *
   * @param envelope the area that will be displayed.
   * @param geoTileSize the size of each tile in world space.
   */
  @Nonnull
  public Coordinate getMinGeoCoordinate(
      final ReferencedEnvelope envelope, final Coordinate geoTileSize) {
    final ReferencedEnvelope tileBounds = getTileBounds();
    final double tileMinX = tileBounds.getMinX();
    double minGeoX = envelope.getMinX();
    double minGeoY = envelope.getMinY();
    double tileMinGeoX =
        (tileMinX + (Math.floor((minGeoX - tileMinX) / geoTileSize.x) * geoTileSize.x));
    final double tileMinY = tileBounds.getMinY();
    double tileMinGeoY =
        (tileMinY + (Math.floor((minGeoY - tileMinY) / geoTileSize.y) * geoTileSize.y));

    return new Coordinate(tileMinGeoX, tileMinGeoY);
  }

  /**
   * Create a buffered image with the correct image bands etc... for the tiles being loaded.
   *
   * @param imageWidth width of the image to create
   * @param imageHeight height of the image to create.
   */
  @Nonnull
  public BufferedImage createBufferedImage(final int imageWidth, final int imageHeight) {
    return new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR);
  }

  /**
   * Create a URL that is common to all tiles for this layer. It may have placeholder like
   * ({matrixId}) if the layer desires. That is up to the layer implementation because the layer is
   * responsible for taking the commonUrl and transforming it to a final tile URI.
   */
  protected String createCommonUrl() throws URISyntaxException, UnsupportedEncodingException {
    return this.params.createCommonUrl();
  }

  /**
   * Return the image to draw in place of a tile that is missing.
   *
   * <p>If this method returns null nothing will be drawn at the location of the missing image.
   */
  @Nullable
  public BufferedImage getMissingTileImage() {
    return null;
  }

  protected final T getParams() {
    return params;
  }

  /**
   * Return the scaling for this tileInformation.
   *
   * @return the aspect ratio
   */
  protected abstract double getImageBufferScaling();
}
