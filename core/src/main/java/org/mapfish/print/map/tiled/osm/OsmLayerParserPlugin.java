package org.mapfish.print.map.tiled.osm;

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
 * <p>Renders OpenStreetMap or other tiled layers.</p>
 * <p>Type: <code>osm</code></p>
 * [[examples=print_osm_new_york_EPSG_3857]]
 */
public final class OsmLayerParserPlugin extends AbstractGridCoverageLayerPlugin
        implements MapLayerFactoryPlugin<OsmLayerParam> {
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private MetricRegistry registry;

    private Set<String> typenames = Sets.newHashSet("osm");

    @Override
    public Set<String> getTypeNames() {
        return this.typenames;
    }

    @Override
    public OsmLayerParam createParameter() {
        return new OsmLayerParam();
    }

    @Nonnull
    @Override
    public OsmLayer parse(
            @Nonnull final Template template,
            @Nonnull final OsmLayerParam param) {
        String styleRef = param.rasterStyle;
        return new OsmLayer(this.forkJoinPool,
                super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                param, this.registry, template.getConfiguration());
    }
}
