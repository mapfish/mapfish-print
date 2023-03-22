package org.mapfish.print.map.geotools.grid;

import java.util.concurrent.ForkJoinPool;
import javax.annotation.Nonnull;
import org.geotools.data.FeatureSource;
import org.geotools.styling.Style;
import org.mapfish.print.OptionalUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayerPlugin;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A layer which is a spatial grid of lines on the map.
 *
 * <p>Type: <code>grid</code> [[examples=crosses_point_grid]]
 */
public final class GridLayerPlugin extends AbstractFeatureSourceLayerPlugin<GridParam> {

  private static final String TYPE = "grid";
  @Autowired private ForkJoinPool pool;

  /** Constructor. */
  public GridLayerPlugin() {
    super(TYPE);
  }

  @Override
  public GridParam createParameter() {
    return new GridParam();
  }

  @Nonnull
  @Override
  public GridLayer parse(@Nonnull final Template template, @Nonnull final GridParam layerData) {
    LabelPositionCollector labels = new LabelPositionCollector();
    FeatureSourceSupplier featureSource = createFeatureSourceFunction(template, layerData, labels);
    final StyleSupplier<FeatureSource> styleFunction = createStyleSupplier(template, layerData);
    return new GridLayer(
        this.pool,
        featureSource,
        styleFunction,
        template.getConfiguration().renderAsSvg(layerData.renderAsSvg),
        layerData,
        labels);
  }

  private StyleSupplier<FeatureSource> createStyleSupplier(
      final Template template, final GridParam layerData) {
    return new StyleSupplier<FeatureSource>() {
      @Override
      public Style load(
          final MfClientHttpRequestFactory requestFactory, final FeatureSource featureSource) {
        String styleRef = layerData.style;
        return OptionalUtils.or(
                () -> template.getStyle(styleRef),
                () ->
                    GridLayerPlugin.super.parser.loadStyle(
                        template.getConfiguration(), requestFactory, styleRef))
            .orElseGet(() -> layerData.gridType.strategy.defaultStyle(template, layerData));
      }
    };
  }

  private FeatureSourceSupplier createFeatureSourceFunction(
      final Template template, final GridParam layerData, final LabelPositionCollector labels) {
    return layerData.gridType.strategy.createFeatureSource(template, layerData, labels);
  }
}
