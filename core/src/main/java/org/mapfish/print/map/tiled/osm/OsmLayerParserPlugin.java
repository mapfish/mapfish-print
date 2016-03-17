package org.mapfish.print.map.tiled.osm;

import com.google.common.collect.Sets;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import javax.annotation.Nonnull;

/**
 * <p>Renders OpenStreetMap or other tiled layers.</p>
 * <p>Type: <code>osm</code></p>
 * [[examples=print_osm_new_york_EPSG_900913]]
 *
 * @author Jesse on 4/3/14.
 */
public final class OsmLayerParserPlugin extends AbstractGridCoverageLayerPlugin implements MapLayerFactoryPlugin<OsmLayerParam> {
    @Autowired
    private StyleParser parser;
    @Autowired
    private ForkJoinPool forkJoinPool;

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
    public OsmLayer parse(@Nonnull final Template template,
                          @Nonnull final OsmLayerParam param) throws Throwable {

        String styleRef = param.rasterStyle;
        return new OsmLayer(this.forkJoinPool,
                super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                param);
    }
}
