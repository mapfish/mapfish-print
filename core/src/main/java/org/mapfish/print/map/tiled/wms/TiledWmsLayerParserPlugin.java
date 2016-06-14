package org.mapfish.print.map.tiled.wms;

import com.google.common.collect.Sets;
import jsr166y.ForkJoinPool;
import org.geotools.coverage.grid.GridCoverage2D;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.geotools.AbstractGridCoverageLayerPlugin;

import java.util.Set;
import javax.annotation.Nonnull;

/**
 * <p>Renders tiled WMS layers.</p>
 * <p>Type: <code>tiledwms</code></p>
 * [[examples=printtiledwms]]
 *
 * @author Stéphane Brunner
 */
public final class TiledWmsLayerParserPlugin extends AbstractGridCoverageLayerPlugin implements MapLayerFactoryPlugin<TiledWmsLayerParam> {

    private final ForkJoinPool forkJoinPool = new ForkJoinPool(this.getMaxNumberParallelRequests());

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
            @Nonnull final TiledWmsLayerParam param) throws Throwable {

        String styleRef = param.rasterStyle;
        return new TiledWmsLayer(this.forkJoinPool,
                super.<GridCoverage2D>createStyleSupplier(template, styleRef),
                param);
    }
}
