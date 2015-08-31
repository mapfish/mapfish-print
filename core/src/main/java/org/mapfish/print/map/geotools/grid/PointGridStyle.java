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
 *
 * @author Jesse on 6/29/2015.
 */
public final class PointGridStyle {

    private static final int CROSS_SIZE = 10;

    private PointGridStyle() {
        // do nothing
    }

    /**
     * Create the Grid Point style.
     */
    static Style get(final GridParam params) {
        StyleBuilder builder = new StyleBuilder();

        Symbolizer pointSymbolizer = crossSymbolizer("shape://plus", builder, CROSS_SIZE, ColorParser.toColor(params.gridColor));
        Symbolizer halo = crossSymbolizer("cross", builder, CROSS_SIZE + params.haloRadius, ColorParser.toColor(params.haloColor));
        final Style style = builder.createStyle(pointSymbolizer);
        final List<Symbolizer> symbolizers = style.featureTypeStyles().get(0).rules().get(0).symbolizers();
        symbolizers.add(0, halo);

        return style;
    }

    private static Symbolizer crossSymbolizer(final String name, final StyleBuilder builder,
                                              final int crossSize, final Color pointColor) {
        Mark cross = builder.createMark(name, pointColor, pointColor, 1);
        Graphic graphic = builder.createGraphic(null, cross, null);
        graphic.setSize(builder.literalExpression(crossSize));

        return builder.createPointSymbolizer(graphic);
    }
}
