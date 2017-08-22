package org.mapfish.print.map.tiled.wmts;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Sets;

import org.geotools.coverage.grid.GridCoverage2D;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nonnull;

/**
 * <p>Renders WMTS layers.</p>
 * <p>Type: <code>wmts</code></p>
 * [[examples=printwmts_tyger_ny_EPSG_3857]]
 */
public final class WmtsLayerParserPlugin extends AbstractGridCoverageLayerPlugin
        implements MapLayerFactoryPlugin<WMTSLayerParam> {
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private MetricRegistry registry;

    private Set<String> typenames = Sets.newHashSet("wmts");

    @Override
    public Set<String> getTypeNames() {
        return this.typenames;
    }

    @Override
    public WMTSLayerParam createParameter() {
        return new WMTSLayerParam();
    }

    @Nonnull
    @Override
    public WMTSLayer parse(
            @Nonnull final Template template,
            @Nonnull final WMTSLayerParam param) {
        String styleRef = param.rasterStyle;
        return new WMTSLayer(this.forkJoinPool,
                super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                param, this.registry, template.getConfiguration());
    }
}
