package org.mapfish.print.map.geotools.grid;

import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.mapfish.print.map.style.json.ColorParser;

import java.awt.Color;
import java.util.List;

/**
 * Creates the Named LineGridStyle.
 */
public final class PointGridStyle {

    private static final double CROSS_SIZE = 10.0;

    private PointGridStyle() {
        // do nothing
    }

    /**
     * Create the Grid Point style.
     */
    static Style get(final GridParam params) {
        final StyleBuilder builder = new StyleBuilder();

        final Symbolizer pointSymbolizer = crossSymbolizer("shape://plus", builder, CROSS_SIZE,
                                                           params.gridColor);
        final Style style = builder.createStyle(pointSymbolizer);
        final List<Symbolizer> symbolizers = style.featureTypeStyles().get(0).rules().get(0).symbolizers();

        if (params.haloRadius > 0.0) {
            Symbolizer halo = crossSymbolizer("cross", builder, CROSS_SIZE + params.haloRadius * 2.0,
                                              params.haloColor);
            symbolizers.add(0, halo);
        }

        return style;
    }

    private static Symbolizer crossSymbolizer(
            final String name, final StyleBuilder builder,
            final double crossSize, final String pointColorTxt) {
        final Color pointColor = ColorParser.toColor(pointColorTxt);
        final Mark cross = builder.createMark(name, pointColor, pointColor, 1);
        final Graphic graphic = builder.createGraphic(null, cross, null);
        graphic.setSize(builder.literalExpression(crossSize));

        return builder.createPointSymbolizer(graphic);
    }
}
