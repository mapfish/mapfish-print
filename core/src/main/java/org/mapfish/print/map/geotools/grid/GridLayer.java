package org.mapfish.print.map.geotools.grid;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.geotools.data.FeatureSource;
import org.geotools.map.Layer;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.HttpRequestCache;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.map.style.json.ColorParser;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

/**
 * A layer which is a spatial grid of lines on the map.
 */
public final class GridLayer implements MapLayer {
    private final GridParam params;
    private final LabelPositionCollector labels;
    AbstractFeatureSourceLayer grid;

    /**
     * Constructor.
     *
     * @param executorService the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be called once.
     * @param styleSupplier a function that creates the style for styling the features. This will only be called once.
     * @param renderAsSvg is the layer rendered as SVG?
     * @param params the parameters for this layer
     * @param labels the grid labels to render
     */
    public GridLayer(final ExecutorService executorService,
                     final FeatureSourceSupplier featureSourceSupplier,
                     final StyleSupplier<FeatureSource> styleSupplier,
                     final boolean renderAsSvg,
                     final GridParam params,
                     final LabelPositionCollector labels) {
        this.grid = new AbstractFeatureSourceLayer(executorService, featureSourceSupplier, styleSupplier, renderAsSvg, params) { };
        this.params = params;
        this.labels = labels;
    }

    @Override
    public Optional<MapLayer> tryAddLayer(final MapLayer newLayer) {
        return Optional.absent();
    }

    @Override
    public double getImageBufferScaling() {
        return 1.0;
    }

    @Override
    public void prepareRender(final MapfishMapContext transformer) {
    }

    @Override
    public void render(final Graphics2D graphics, final MfClientHttpRequestFactory clientHttpRequestFactory,
                       final MapfishMapContext transformer, final String jobId) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        int haloRadius = this.params.haloRadius;
        double dpiScaling = transformer.getDPI() / Constants.PDF_DPI;

        this.grid.render(graphics2D, clientHttpRequestFactory, transformer, jobId);
        Font baseFont = null;
        for (String fontName : this.params.font.name) {
            try {
                baseFont = new Font(fontName, this.params.font.style.styleId, (int) (this.params.font.size * dpiScaling));
                break;
            } catch (Exception e) {
                // try next font in list
            }
        }

        graphics2D.setFont(baseFont);
        int halfCharHeight = (graphics2D.getFontMetrics().getAscent() / 2);
        Stroke baseStroke = graphics2D.getStroke();
        AffineTransform baseTransform = graphics2D.getTransform();
        Color haloColor = ColorParser.toColor(this.params.haloColor);
        Color labelColor = ColorParser.toColor(this.params.labelColor);

        for (GridLabel label : this.labels) {
            Shape textShape = baseFont.createGlyphVector(graphics2D.getFontRenderContext(), label.text).getOutline();

            Rectangle2D textBounds = textShape.getBounds2D();
            AffineTransform transform = new AffineTransform(baseTransform);
            transform.translate(label.x, label.y);

            applyOffset(transform, label.side);

            RotationQuadrant.getQuadrant(transformer.getRotation(), this.params.rotateLabels)
                    .updateTransform(transform, this.params.indent, label.side,
                            halfCharHeight, textBounds);
            graphics2D.setTransform(transform);

            if (haloRadius > 0) {
                graphics2D.setStroke(new BasicStroke(2 * haloRadius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics2D.setColor(haloColor);
                graphics2D.draw(textShape);
            }

            graphics2D.setStroke(baseStroke);
            graphics2D.setColor(labelColor);
            graphics2D.fill(textShape);
        }
    }

    private void applyOffset(final AffineTransform transform, final GridLabel.Side side) {
        switch (side) {
            case BOTTOM:
            case TOP:
                transform.translate(this.params.verticalXOffset, 0);
                break;
            default:
                transform.translate(0, this.params.horizontalYOffset);
        }
    }

    @Override
    public boolean supportsNativeRotation() {
        return true;
    }

    @Override
    public String getName() {
        return this.params.name;
    }

    @VisibleForTesting
    List<? extends Layer> getLayers(
            @Nonnull final MfClientHttpRequestFactory httpRequestFactory,
            @Nonnull final MapfishMapContext mapContext,
            @Nonnull final String jobId) throws Exception {
        return this.grid.getLayers(httpRequestFactory, mapContext, jobId);
    }

    @Override
    public RenderType getRenderType() {
        return this.grid.getRenderType();
    }

    @Override
    public void cacheResources(final HttpRequestCache httpRequestCache,
                               final MfClientHttpRequestFactory clientHttpRequestFactory, final MapfishMapContext transformer,
                               final String jobId) {
        this.grid.cacheResources(httpRequestCache, clientHttpRequestFactory, transformer, jobId);
    }
}
