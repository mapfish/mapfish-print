package org.mapfish.print.map.tiled.wmts;

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
 * Renders WMTS layers.
 *
 * <p>Type: <code>wmts</code> [[examples=printwmts_tyger_ny_EPSG_3857]]
 */
public final class WmtsLayerParserPlugin extends AbstractGridCoverageLayerPlugin
    implements MapLayerFactoryPlugin<WMTSLayerParam> {
  private static final Set<String> TYPENAMES = Collections.singleton("wmts");
  @Autowired private ForkJoinPool forkJoinPool;
  @Autowired private MetricRegistry registry;

  @Override
  public Set<String> getTypeNames() {
    return TYPENAMES;
  }

  @Override
  public WMTSLayerParam createParameter() {
    return new WMTSLayerParam();
  }

  @Nonnull
  @Override
  public WMTSLayer parse(@Nonnull final Template template, @Nonnull final WMTSLayerParam param) {
    String styleRef = param.rasterStyle;
    return new WMTSLayer(
        this.forkJoinPool,
        super.<GridCoverage2D>createStyleSupplier(template, styleRef),
        param,
        this.registry,
        template.getConfiguration());
  }
}
