package org.mapfish.print.map.image.wms;

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
 * Renders WMS layers as single image.
 *
 * <p>Type: <code>wms</code>
 * [[examples=printwms_tyger_ny_EPSG_3857,printwms_UsaPopulation_EPSG_4326]]
 */
public final class WmsLayerFactoryPlugin extends AbstractGridCoverageLayerPlugin
    implements MapLayerFactoryPlugin<WmsLayerParam> {
  private static final String TYPE = "wms";
  @Autowired private ForkJoinPool forkJoinPool;
  @Autowired private MetricRegistry metricRegistry;

  @Override
  public Set<String> getTypeNames() {
    return Collections.singleton(TYPE);
  }

  @Override
  public WmsLayerParam createParameter() {
    return new WmsLayerParam();
  }

  @Nonnull
  @Override
  public WmsLayer parse(@Nonnull final Template template, @Nonnull final WmsLayerParam layerData) {
    String styleRef = layerData.rasterStyle;
    return new WmsLayer(
        this.forkJoinPool,
        super.<GridCoverage2D>createStyleSupplier(template, styleRef),
        layerData,
        this.metricRegistry,
        template.getConfiguration());
  }
}
