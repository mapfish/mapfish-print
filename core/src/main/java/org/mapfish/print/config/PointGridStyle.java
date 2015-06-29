package org.mapfish.print.config;

import org.geotools.styling.Graphic;
import org.geotools.styling.SLD;
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
     *
     * @return
     */
    public static Style get() {
        StyleBuilder builder = new StyleBuilder();

        //CSOFF:MagicNumber
        final Color textColor = new Color(50, 50, 255);
        final Color pointColor = SLD.toColor("dddddd");
        Graphic graphic = builder.createGraphic(null, builder.createMark("cross", pointColor, 1), null);
        graphic.setSize(builder.literalExpression(12));
        //CSON:MagicNumber

        Symbolizer pointSymbolizer = builder.createPointSymbolizer(graphic);
        final Style style = builder.createStyle(pointSymbolizer);
        final List<Symbolizer> symbolizers = style.featureTypeStyles().get(0).rules().get(0).symbolizers();
        symbolizers.add(0, LineGridStyle.createGridTextSymbolizer(builder, textColor));

        return style;
    }
}
