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

    private PointGridStyle() {
        // do nothing
    }

    /**
     * Create the Grid Point style.
     */
    public static Style get() {
        StyleBuilder builder = new StyleBuilder();

        final Color textColor = Color.darkGray;
        final Color pointColor = Color.gray;
        Mark cross = builder.createMark("shape://plus", pointColor, pointColor, 1);
        Graphic graphic = builder.createGraphic(null, cross, null);

        //CSOFF:MagicNumber
        graphic.setSize(builder.literalExpression(10));
        //CSON:MagicNumber

        Symbolizer pointSymbolizer = builder.createPointSymbolizer(graphic);
        final Style style = builder.createStyle(pointSymbolizer);
        final List<Symbolizer> symbolizers = style.featureTypeStyles().get(0).rules().get(0).symbolizers();
        symbolizers.add(0, LineGridStyle.createGridTextSymbolizer(builder, textColor));

        return style;
    }
}
