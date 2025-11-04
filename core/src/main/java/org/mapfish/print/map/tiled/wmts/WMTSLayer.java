package org.mapfish.print.map.tiled.wmts;

import static org.mapfish.print.Constants.OGC_DPI;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.EPSG3857Utils;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.tiled.AbstractTiledLayer;
import org.mapfish.print.map.tiled.TileInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;


/** Class for loading data from a WMTS. */
public class WMTSLayer extends AbstractTiledLayer<WMTSLayerParam> {
  private static final Logger LOGGER = LoggerFactory.getLogger(WMTSLayer.class);
  private final WMTSLayerParam param;

  /**
   * Constructor.
   *
   * @param forkJoinPool the thread pool for doing the rendering.
   * @param styleSupplier strategy for loading the style for this layer
   * @param param the information needed to create WMTS requests.
   * @param registry the metrics registry.
   * @param configuration the configuration.
   */
  protected WMTSLayer(
      @Nullable final ForkJoinPool forkJoinPool,
      @Nullable final StyleSupplier<GridCoverage2D> styleSupplier,
      @Nonnull final WMTSLayerParam param,
      @Nullable final MetricRegistry registry,
      @Nonnull final Configuration configuration) {
    super(forkJoinPool, styleSupplier, param, registry, configuration);
    this.param = param;
  }

  @Override
  protected final TileInformation<WMTSLayerParam> createTileInformation(
      final MapBounds bounds, final Rectangle paintArea, final double dpi) {
    return new WMTSTileInfo(bounds, paintArea, dpi, this);
  }

  @Override
  public final RenderType getRenderType() {
    return RenderType.fromMimeType(this.param.imageFormat);
  }

  @VisibleForTesting
  static final class WMTSTileInfo extends TileInformation<WMTSLayerParam> {
    private Matrix matrix;
    private final double imageBufferScaling;

    private WMTSTileInfo(
        final MapBounds bounds,
        final Rectangle paintArea,
        final double dpi,
        final WMTSLayer layer) {
      super(bounds, paintArea, dpi, layer.param);
      double diff = Double.POSITIVE_INFINITY;
      final double targetResolution = bounds.getScale(paintArea, dpi).getResolution();
      double ibf = DEFAULT_SCALING;
      LOGGER.debug(
          "Computing imageBufferScaling of layer {} at target resolution {}",
          layer.getName(),
          targetResolution);
      CoordinateReferenceSystem crs = this.bounds.getProjection();
        // The scalingFactor is used to correct for the distortion of the EPSG:3857 (Web Mercator)
        // projection. This projection distorts distances and areas as one moves away from the equator.
        // This factor ensures that the final printed map's scale is accurate at the map's center.
        double scalingFactor = 1;
        if (EPSG3857Utils.is3857(crs)){
            scalingFactor = EPSG3857Utils.computeScalingFactor(bounds);
        }

      for (Matrix m : getParams().matrices) {
        double resolution = m.getResolution(crs);
        LOGGER.debug(
            "Checking tile resolution {} ({} scaling)", resolution, (targetResolution / resolution) * scalingFactor);
        final double delta = Math.abs(resolution - targetResolution);
        if (delta < diff) {
          diff = delta;
          this.matrix = m;
          // Apply the scaling factor to compensate for Mercator projection distortion.
          ibf = (targetResolution / resolution) * scalingFactor;
        }
      }
      imageBufferScaling = ibf;
      LOGGER.debug("The best imageBufferScaling is {}", ibf);

      if (this.matrix == null) {
        throw new IllegalArgumentException(
            "Unable to find a matrix for the resolution: " + targetResolution);
      }
    }

    @Override
    public double getImageBufferScaling() {
      return imageBufferScaling;
    }

    @Override
    @Nonnull
    public Dimension getTileSize() {
      int width = this.matrix.getTileWidth();
      int height = this.matrix.getTileHeight();
      return new Dimension(width, height);
    }

    @Nonnull
    @Override
    protected ReferencedEnvelope getTileBounds() {
      double resolution = getResolution();
      double minX = this.matrix.topLeftCorner[0];
      double tileHeight = this.matrix.getTileHeight();
      double numYTiles = this.matrix.matrixSize[1];
      double minY = this.matrix.topLeftCorner[1] - (tileHeight * numYTiles * resolution);
      double tileWidth = this.matrix.getTileWidth();
      double numXTiles = this.matrix.matrixSize[0];
      double maxX = this.matrix.topLeftCorner[0] + (tileWidth * numXTiles * resolution);
      double maxY = this.matrix.topLeftCorner[1];
      return new ReferencedEnvelope(minX, maxX, minY, maxY, bounds.getProjection());
    }

    @Override
    @Nonnull
    public ClientHttpRequest getTileRequest(
        final MfClientHttpRequestFactory httpRequestFactory,
        final String commonUrl,
        final ReferencedEnvelope tileBounds,
        final Dimension tileSizeOnScreen,
        final int column,
        final int row)
        throws URISyntaxException, IOException {
      URI uri;
      if (RequestEncoding.REST == getParams().requestEncoding) {
        uri = getParams().createRestURI(this.matrix.identifier, row, column);
      } else {
        URI commonUri = new URI(commonUrl);
        uri = getParams().createKVPUri(commonUri, row, column, this.matrix.identifier);
      }
      return httpRequestFactory.createRequest(uri, HttpMethod.GET);
    }

    @Override
    public double getResolution() {
      return this.matrix.getResolution(this.bounds.getProjection());
    }

    @Override
    public Double getLayerDpi() {
      return OGC_DPI;
    }
  }
}
