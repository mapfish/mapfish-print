package org.mapfish.print.map.tiled.wms;

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
 * <p>Renders tiled WMS layers.</p>
 * <p>Type: <code>tiledwms</code></p>
 * [[examples=printtiledwms]]
 */
public final class TiledWmsLayerParserPlugin extends AbstractGridCoverageLayerPlugin
        implements MapLayerFactoryPlugin<TiledWmsLayerParam> {

    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private MetricRegistry registry;

    private final Set<String> typenames = Sets.newHashSet("tiledwms");

    @Override
    public Set<String> getTypeNames() {
        return this.typenames;
    }

    @Override
    public TiledWmsLayerParam createParameter() {
        return new TiledWmsLayerParam();
    }

    @Nonnull
    @Override
    public TiledWmsLayer parse(
            @Nonnull final Template template,
            @Nonnull final TiledWmsLayerParam param) {
        String styleRef = param.rasterStyle;
        return new TiledWmsLayer(this.forkJoinPool,
                super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                param, this.registry, template.getConfiguration());
    }
}
