package org.mapfish.print.map.geotools;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.RescaleStyleVisitor;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.mapfish.print.processor.Processor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

import static org.mapfish.print.Constants.PDF_DPI;

/**
 * A layer that wraps a Geotools Feature Source and a style object.
 */
public abstract class AbstractFeatureSourceLayer extends AbstractGeotoolsLayer {

    private final Boolean renderAsSvg;
    private FeatureSourceSupplier featureSourceSupplier;
    private FeatureSource<?, ?> featureSource = null;
    private StyleSupplier<FeatureSource> styleSupplier;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be
     *         called once.
     * @param styleSupplier a function that creates the style for styling the features. This will only
     *         be called once.
     * @param renderAsSvg is the layer rendered as SVG?
     * @param params the parameters for this layer
     */
    public AbstractFeatureSourceLayer(
            final ExecutorService executorService,
            final FeatureSourceSupplier featureSourceSupplier,
            final StyleSupplier<FeatureSource> styleSupplier,
            final boolean renderAsSvg,
            final AbstractLayerParams params) {
        super(executorService, params);
        this.featureSourceSupplier = featureSourceSupplier;
        this.styleSupplier = styleSupplier;

        this.renderAsSvg = renderAsSvg;
    }

    @SuppressWarnings("unchecked")
    public final void setStyle(final StyleSupplier style) {
        this.styleSupplier = style;
    }

    /**
     * Get the feature source (either load from the supplier or return the cached source).
     *
     * @param httpRequestFactory The factory for making http requests.
     * @param mapContext The map context.
     */
    public final FeatureSource<?, ?> getFeatureSource(
            @Nonnull final MfClientHttpRequestFactory httpRequestFactory,
            @Nonnull final MapfishMapContext mapContext) {
        if (this.featureSource == null) {
            this.featureSource = this.featureSourceSupplier.load(httpRequestFactory, mapContext);
        }
        return this.featureSource;
    }

    @Override
    public final List<? extends Layer> getLayers(
            @Nonnull final MfClientHttpRequestFactory httpRequestFactory,
            @Nonnull final MapfishMapContext mapContext, @Nonnull final Processor.ExecutionContext context) {
        FeatureSource<?, ?> source = getFeatureSource(httpRequestFactory, mapContext);
        Style style = this.styleSupplier.load(httpRequestFactory, source);

        if (mapContext.isDpiSensitiveStyle()) {
            // rescale styles for a higher dpi print
            double scaleFactor = mapContext.getDPI() / PDF_DPI;
            RescaleStyleVisitor scale = new RescaleStyleVisitor(scaleFactor);
            style.accept(scale);
            style = (Style) scale.getCopy();
        }

        return Collections.singletonList(new FeatureLayer(source, style));
    }

    public final void setFeatureCollection(final SimpleFeatureCollection featureCollection) {
        this.featureSourceSupplier = new FeatureSourceSupplier() {
            @Nonnull
            @Override
            public FeatureSource load(
                    @Nonnull final MfClientHttpRequestFactory requestFactory,
                    @Nonnull final MapfishMapContext mapContext) {
                // GeoTools is not always thread safe. In particular the DefaultFeatureCollection.getBounds
                // method. If multiple maps are sharing the same features, this would cause zoomToFeature
                // to result in funky bboxes. To avoid that, we use a copy of the featureCollection
                final SimpleFeatureCollection copy = DataUtilities.collection(featureCollection);
                return new CollectionFeatureSource(copy);
            }
        };
    }

    @Override
    public final RenderType getRenderType() {
        return this.renderAsSvg ? RenderType.SVG : RenderType.UNKNOWN;
    }

    @Override
    public final double getImageBufferScaling() {
        return 1;
    }
}
