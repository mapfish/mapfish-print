package org.mapfish.print.map.geotools;

import com.google.common.base.Function;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * The AbstractGridCoverage2DReaderLayer class.
 */
public abstract class AbstractGridCoverage2DReaderLayer extends AbstractGeotoolsLayer {

    private final Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> coverage2DReaderSupplier;
    private final StyleSupplier<AbstractGridCoverage2DReader> styleSupplier;

    /**
     * Constructor.
     *
     * @param coverage2DReader the coverage2DReader for reading the grid coverage data.
     * @param style style to use for rendering the data.
     * @param executorService the thread pool for doing the rendering.
     * @param params the parameters for this layer
     */
    public AbstractGridCoverage2DReaderLayer(final Function<MfClientHttpRequestFactory, AbstractGridCoverage2DReader> coverage2DReader,
                                             final StyleSupplier<AbstractGridCoverage2DReader> style,
                                             final ExecutorService executorService,
                                             final AbstractLayerParams params) {
        super(executorService, params);
        this.styleSupplier = style;
        this.coverage2DReaderSupplier = coverage2DReader;
    }

    @Override
    public final double getImageBufferScaling() {
        return 1;
    }

    @Override
    public final synchronized List<? extends Layer> getLayers(final MfClientHttpRequestFactory httpRequestFactory,
                                                              final MapfishMapContext mapContext,
                                                              final String jobId) throws Exception {
        AbstractGridCoverage2DReader coverage2DReader = this.coverage2DReaderSupplier.apply(httpRequestFactory);
        Style style = this.styleSupplier.load(httpRequestFactory, coverage2DReader);
        return Collections.singletonList(new GridReaderLayer(coverage2DReader, style));
    }
}
