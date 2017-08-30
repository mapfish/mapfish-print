package org.mapfish.print.map.image;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.map.geotools.AbstractGeotoolsLayer;
import org.mapfish.print.map.geotools.StyleSupplier;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Common implementation for layers that are represented as a single grid coverage image.
 */
public abstract class AbstractSingleImageLayer extends AbstractGeotoolsLayer {

    private final StyleSupplier<GridCoverage2D> styleSupplier;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param styleSupplier the style to use when drawing the constructed grid coverage on the map.
     * @param params the parameters for this layer
     */
    protected AbstractSingleImageLayer(final ExecutorService executorService,
                                       final StyleSupplier<GridCoverage2D> styleSupplier,
                                       final AbstractLayerParams params) {
        super(executorService, params);
        this.styleSupplier = styleSupplier;
    }

    @Override
    protected final List<? extends Layer> getLayers(final MfClientHttpRequestFactory httpRequestFactory,
                                                    final MapfishMapContext mapContext,
                                                    final String jobId) throws Exception {
        BufferedImage image;
        try {
            image = loadImage(httpRequestFactory, mapContext);
        } catch (Throwable t) {
            throw ExceptionUtils.getRuntimeException(t);
        }

        final MapBounds bounds = mapContext.getBounds();
        final ReferencedEnvelope mapEnvelope = bounds.toReferencedEnvelope(mapContext.getPaintArea());

        GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);
        GeneralEnvelope gridEnvelope = new GeneralEnvelope(mapEnvelope.getCoordinateReferenceSystem());
        gridEnvelope.setEnvelope(mapEnvelope.getMinX(), mapEnvelope.getMinY(),
                mapEnvelope.getMaxX(), mapEnvelope.getMaxY());
        final String coverageName = getClass().getSimpleName();
        final GridCoverage2D gridCoverage2D = factory.create(coverageName, image, gridEnvelope,
                null, null, null);

        Style style = this.styleSupplier.load(httpRequestFactory, gridCoverage2D);
        return Collections.singletonList(new GridCoverageLayer(gridCoverage2D, style));
    }

    /**
     * Load the image at the requested size for the provided map bounds.
     * @param requestFactory the factory to use for making http requests
     * @param transformer object containing map rendering information
     */
    protected abstract BufferedImage loadImage(MfClientHttpRequestFactory requestFactory,
                                               MapfishMapContext transformer) throws Throwable;

    @Override
    public final double getImageBufferScaling() {
        return 1;
    }
}
