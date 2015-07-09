package org.mapfish.print.config;

import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;

import java.awt.Color;
import java.util.List;

/**
 * Creates the Named LineGridStyle.
 *
 * @author Jesse on 6/29/2015.
 */
public final class PointGridStyle {

    private static final int CROSS_SIZE = 10;
    private static final int HALO_SIZE = 12;

    private PointGridStyle() {
        // do nothing
    }

    /**
     * Create the Grid Point style.
     */
    public static Style get() {
        StyleBuilder builder = new StyleBuilder();

        final Color textColor = Color.darkGray;
        Symbolizer pointSymbolizer = crossSymbolizer("shape://plus", builder, CROSS_SIZE, Color.gray);
        Symbolizer halo = crossSymbolizer("cross", builder, HALO_SIZE, Color.white);
        final Style style = builder.createStyle(pointSymbolizer);
        final List<Symbolizer> symbolizers = style.featureTypeStyles().get(0).rules().get(0).symbolizers();
        symbolizers.add(0, halo);
        symbolizers.add(0, LineGridStyle.createGridTextSymbolizer(builder, textColor));

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
