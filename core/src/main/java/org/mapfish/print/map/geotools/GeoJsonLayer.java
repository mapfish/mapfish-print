package org.mapfish.print.map.geotools;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import jakarta.annotation.Nonnull;
import org.geotools.api.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.mapfish.print.PrintException;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.parser.HasDefaultValue;

/**
 * Parses GeoJSON from the request data.
 *
 * <p>Type: <code>geojson</code>
 */
public final class GeoJsonLayer extends AbstractFeatureSourceLayer {

  /**
   * Constructor.
   *
   * @param executorService the thread pool for doing the rendering.
   * @param featureSourceSupplier a function that creates the feature source. This will only be
   *     called once.
   * @param styleSupplier a function that creates the style for styling the features. This will only
   *     be called once.
   * @param renderAsSvg is the layer rendered as SVG?
   * @param params the parameters for this layer
   */
  public GeoJsonLayer(
      final ExecutorService executorService,
      final FeatureSourceSupplier featureSourceSupplier,
      final StyleSupplier<FeatureSource<?, ?>> styleSupplier,
      final boolean renderAsSvg,
      final AbstractLayerParams params) {
    super(executorService, featureSourceSupplier, styleSupplier, renderAsSvg, params);
  }

  @Override
  public LayerContext prepareRender(
      final MapfishMapContext transformer,
      final MfClientHttpRequestFactory clientHttpRequestFactory) {
    return new LayerContext(DEFAULT_SCALING, null, null);
  }

  /**
   * Renders GeoJSON layers.
   *
   * <p>Type: <code>geojson</code>
   * [[examples=json_styling,datasource_multiple_maps,printwms_tyger_ny_EPSG_3857]]
   */
  public static final class Plugin extends AbstractFeatureSourceLayerPlugin<GeoJsonParam> {

    private static final String TYPE = "geojson";
    private static final String COMPATIBILITY_TYPE = "vector";

    /** Constructor. */
    public Plugin() {
      super(TYPE, COMPATIBILITY_TYPE);
    }

    @Override
    public GeoJsonParam createParameter() {
      return new GeoJsonParam();
    }

    @Nonnull
    @Override
    public GeoJsonLayer parse(@Nonnull final Template template, @Nonnull final GeoJsonParam param) {
      return new GeoJsonLayer(
          this.forkJoinPool,
          createFeatureSourceSupplier(template, param.geoJson),
          createStyleFunction(template, param.style),
          template.getConfiguration().renderAsSvg(param.renderAsSvg),
          param);
    }

    private FeatureSourceSupplier createFeatureSourceSupplier(
        final Template template, final String geoJsonString) {
      return new FeatureSourceSupplier() {
        @Nonnull
        @Override
        public FeatureSource<?, ?> load(
            @Nonnull final MfClientHttpRequestFactory requestFactory,
            @Nonnull final MapfishMapContext mapContext) {
          final FeaturesParser parser =
              new FeaturesParser(requestFactory, mapContext.isForceLongitudeFirst());
          SimpleFeatureCollection featureCollection;
          try {
            featureCollection = parser.autoTreat(template, geoJsonString);
            return new CollectionFeatureSource(featureCollection);
          } catch (IOException e) {
            throw new PrintException("Failed to load " + mapContext, e);
          }
        }
      };
    }
  }

  /** The parameters for creating a layer that renders GeoJSON formatted data. */
  public static class GeoJsonParam extends AbstractVectorLayerParam {
    /**
     * A geojson formatted string or url to the geoJson or the raw GeoJSON data.
     *
     * <p>The url can be a file url, however if it is it must be relative to the configuration
     * directory.
     */
    @HasDefaultValue public String geoJson = "{\"type\": \"FeatureCollection\", \"features\": []}";
  }
}
