/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.geotools.grid;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.geotools.data.FeatureSource;
import org.geotools.map.Layer;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
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

/**
 * A layer which is a spatial grid of lines on the map.
 *
 * @author Jesse on 7/2/2014.
 */
public final class GridLayer implements MapLayer {
    private final GridParam params;
    private final LabelPositionCollector labels;
    AbstractFeatureSourceLayer grid;

    /**
     * Constructor.
     *
     * @param executorService       the thread pool for doing the rendering.
     * @param featureSourceSupplier a function that creates the feature source.  This will only be called once.
     * @param styleSupplier         a function that creates the style for styling the features. This will only be called once.
     * @param renderAsSvg           is the layer rendered as SVG?
     * @param params                the parameters for this layer
     * @param labels                the grid labels to render
     */
    public GridLayer(final ExecutorService executorService,
                     final FeatureSourceSupplier featureSourceSupplier,
                     final StyleSupplier<FeatureSource> styleSupplier,
                     final boolean renderAsSvg,
                     final GridParam params,
                     final LabelPositionCollector labels) {
        this.grid = new AbstractFeatureSourceLayer(executorService, featureSourceSupplier, styleSupplier, renderAsSvg, params) {
        };
        this.params = params;
        this.labels = labels;
    }

    @Override
    public Optional<MapLayer> tryAddLayer(final MapLayer newLayer) {
        return Optional.absent();
    }

    @Override
    public void render(final Graphics2D graphics, final MfClientHttpRequestFactory clientHttpRequestFactory,
                       final MapfishMapContext transformer, final boolean isFirstLayer) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        int haloRadius = this.params.haloRadius;
        int charHeight = (graphics2D.getFontMetrics().getAscent() / 2);
        this.grid.render(graphics2D, clientHttpRequestFactory, transformer, isFirstLayer);
        Font baseFont = graphics2D.getFont();
        if (this.params.font != null) {
            baseFont = new Font(this.params.font.name, this.params.font.style.styleId, this.params.font.size);
        }
        Stroke baseStroke = graphics2D.getStroke();
        AffineTransform baseTransform = graphics2D.getTransform();
        Color haloColor = ColorParser.toColor(this.params.haloColor);
        Color labelColor = ColorParser.toColor(this.params.labelColor);

        for (GridLabel label : this.labels) {
            Shape textShape = baseFont.createGlyphVector(graphics2D.getFontRenderContext(), label.text).getOutline();

            Rectangle2D textBounds = textShape.getBounds2D();
            AffineTransform transform = new AffineTransform(baseTransform);
            transform.translate(label.x, label.y);

            double rotationDegrees = Math.toDegrees(transformer.getRotation());
            RotationQuadrant.getQuadrant(rotationDegrees).updateTransform(transform, this.params.indent, label.side,
                    charHeight, textBounds);
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

    @Override
    public boolean supportsNativeRotation() {
        return true;
    }

    @Override
    public String getName() {
        return this.params.name;
    }

    @VisibleForTesting
    List<? extends Layer> getLayers(final MfClientHttpRequestFactory httpRequestFactory,
                                    final MapfishMapContext mapContext,
                                    final boolean isFirstLayer) throws Exception {
        return this.grid.getLayers(httpRequestFactory, mapContext, isFirstLayer);
    }
}
