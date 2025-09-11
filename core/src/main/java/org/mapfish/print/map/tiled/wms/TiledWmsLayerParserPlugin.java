package org.mapfish.print.map.tiled.wms;

import com.codahale.metrics.MetricRegistry;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import jakarta.annotation.Nonnull;
import org.geotools.coverage.grid.GridCoverage2D;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Renders tiled WMS layers.
 *
 * <p>Type: <code>tiledwms</code> [[examples=printtiledwms]]
 */
public final class TiledWmsLayerParserPlugin extends AbstractGridCoverageLayerPlugin
    implements MapLayerFactoryPlugin<TiledWmsLayerParam> {

  private static final Set<String> TYPENAMES = Collections.singleton("tiledwms");
  @Autowired private ForkJoinPool forkJoinPool;
  @Autowired private MetricRegistry registry;

  @Override
  public Set<String> getTypeNames() {
    return TYPENAMES;
  }

  @Override
  public TiledWmsLayerParam createParameter() {
    return new TiledWmsLayerParam();
  }

  @Nonnull
  @Override
  public TiledWmsLayer parse(
      @Nonnull final Template template, @Nonnull final TiledWmsLayerParam param) {
    String styleRef = param.rasterStyle;
    return new TiledWmsLayer(
        this.forkJoinPool,
        super.<GridCoverage2D>createStyleSupplier(template, styleRef),
        param,
        this.registry,
        template.getConfiguration());
  }
}
