package org.mapfish.print.map.tiled.osm;

import com.codahale.metrics.MetricRegistry;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.URIUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.tiled.AbstractTiledLayer;
import org.mapfish.print.map.tiled.TileCacheInformation;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

/** Strategy object for rendering Osm based layers. */
public final class OsmLayer extends AbstractTiledLayer<OsmLayerParam> {
  private final OsmLayerParam param;

  /**
   * Constructor.
   *
   * @param forkJoinPool the thread pool for doing the rendering.
   * @param styleSupplier strategy for loading the style for this layer.
   * @param param the information needed to create OSM requests.
   * @param registry the metrics registry.
   * @param configuration the configuration.
   */
  public OsmLayer(
      @Nonnull final ForkJoinPool forkJoinPool,
      @Nonnull final StyleSupplier<GridCoverage2D> styleSupplier,
      @Nonnull final OsmLayerParam param,
      @Nonnull final MetricRegistry registry,
      @Nonnull final Configuration configuration) {
    super(forkJoinPool, styleSupplier, param, registry, configuration);
    this.param = param;
  }

  @Override
  protected TileCacheInformation<OsmLayerParam> createTileInformation(
      final MapBounds bounds, final Rectangle paintArea, final double dpi) {
    return new OsmTileCacheInformation(bounds, paintArea, dpi, this.param);
  }

  @Override
  public RenderType getRenderType() {
    return RenderType.fromFileExtension(this.param.imageExtension);
  }

  private static final class OsmTileCacheInformation extends TileCacheInformation<OsmLayerParam> {
    private final double resolution;
    private final int resolutionIndex;
    private final double imageBufferScaling;

    private OsmTileCacheInformation(
        final MapBounds bounds,
        final Rectangle paintArea,
        final double dpi,
        final OsmLayerParam layerParameter) {
      super(bounds, paintArea, dpi, layerParameter);

      final double targetResolution = bounds.getScale(paintArea, dpi).getResolution();
      double ibf = DEFAULT_SCALING;
      Double[] resolutions = getParams().resolutions;
      int pos = resolutions.length - 1;
      double result = resolutions[pos];
      for (int i = resolutions.length - 1; i >= 0; --i) {
        double cur = resolutions[i];
        if (cur <= targetResolution * getParams().resolutionTolerance) {
          result = cur;
          pos = i;
          ibf = cur / targetResolution;
        }
      }
      imageBufferScaling = ibf;
      this.resolution = result;
      this.resolutionIndex = pos;
    }

    @Override
    public double getImageBufferScaling() {
      return imageBufferScaling;
    }

    @Nonnull
    @Override
    public ClientHttpRequest getTileRequest(
        final MfClientHttpRequestFactory httpRequestFactory,
        final String commonUrl,
        final ReferencedEnvelope tileBounds,
        final Dimension tileSizeOnScreen,
        final int column,
        final int row)
        throws IOException, URISyntaxException {

      final URI uri;
      if (commonUrl.contains("{x}")
          && commonUrl.contains("{z}")
          && (commonUrl.contains("{y}") || commonUrl.contains("{-y}"))) {
        String url =
            commonUrl
                .replace("{z}", Integer.toString(this.resolutionIndex))
                .replace("{x}", Integer.toString(column))
                .replace("{y}", Integer.toString(row));
        if (commonUrl.contains("{-y}")) {
          // {-y} is for  OSGeo TMS layers, see also: https://josm.openstreetmap
          // .de/wiki/Maps#TileMapServicesTMS
          url =
              url.replace(
                  "{-y}", Integer.toString((int) Math.pow(2, this.resolutionIndex) - 1 - row));
        }
        uri = new URI(url);
      } else {
        StringBuilder path = new StringBuilder();
        if (!commonUrl.endsWith("/")) {
          path.append('/');
        }
        path.append(this.resolutionIndex);
        path.append('/').append(column);
        path.append('/').append(row);
        path.append('.').append(getParams().imageExtension);

        uri = new URI(commonUrl + path);
      }

      return httpRequestFactory.createRequest(
          URIUtils.addParams(
              uri,
              OsmLayerParam.convertToMultiMap(getParams().customParams),
              URIUtils.getParameters(uri).keySet()),
          HttpMethod.GET);
    }

    @Override
    public double getResolution() {
      return this.resolution;
    }

    @Override
    public Double getLayerDpi() {
      return getParams().dpi;
    }

    @Override
    public Dimension getTileSize() {
      return getParams().getTileSize();
    }

    @Nonnull
    @Override
    protected ReferencedEnvelope getTileCacheBounds() {
      return new ReferencedEnvelope(getParams().getMaxExtent(), this.bounds.getProjection());
    }
  }
}
