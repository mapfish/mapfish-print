package org.mapfish.print.map.tiled.osm;

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
 * Renders OpenStreetMap or other tiled layers.
 *
 * <p>Type: <code>osm</code> [[examples=print_osm_new_york_EPSG_3857]]
 */
public final class OsmLayerParserPlugin extends AbstractGridCoverageLayerPlugin
    implements MapLayerFactoryPlugin<OsmLayerParam> {
  private static final Set<String> TYPENAMES = Collections.singleton("osm");
  @Autowired private ForkJoinPool forkJoinPool;
  @Autowired private MetricRegistry registry;

  @Override
  public Set<String> getTypeNames() {
    return TYPENAMES;
  }

  @Override
  public OsmLayerParam createParameter() {
    return new OsmLayerParam();
  }

  @Nonnull
  @Override
  public OsmLayer parse(@Nonnull final Template template, @Nonnull final OsmLayerParam param) {
    String styleRef = param.rasterStyle;
    return new OsmLayer(
        this.forkJoinPool,
        super.<GridCoverage2D>createStyleSupplier(template, styleRef),
        param,
        this.registry,
        template.getConfiguration());
  }
}
