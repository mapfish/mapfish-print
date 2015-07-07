package org.mapfish.print.config;

import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.mapfish.print.Constants;
import org.opengis.filter.expression.Expression;

import java.awt.Color;
import java.util.List;

/**
 * Creates the Named LineGridStyle.
 *
 * @author Jesse on 6/29/2015.
 */
public final class LineGridStyle {
    private LineGridStyle() {
        // do nothing
    }

    /**
     * Gets the line grid style.
     */
    public static Style get() {
        return createGridStyle(new StyleBuilder());
    }

    private static Style createGridStyle(final StyleBuilder builder) {
        final LineSymbolizer lineSymbolizer = builder.createLineSymbolizer();
        //CSOFF:MagicNumber
        final Color strokeColor = new Color(127, 127, 255);
        final Color textColor = new Color(50, 50, 255);
        lineSymbolizer.setStroke(builder.createStroke(strokeColor, 1, new float[]{4f, 4f}));
        //CSON:MagicNumber

        final Style style = builder.createStyle(lineSymbolizer);
        final List<Symbolizer> symbolizers = style.featureTypeStyles().get(0).rules().get(0).symbolizers();
        symbolizers.add(0, createGridTextSymbolizer(builder, textColor));
        return style;
    }

    static TextSymbolizer createGridTextSymbolizer(final StyleBuilder builder,
                                                    final Color color) {
        final double yAnchorPixels = 0.5;
        Expression xDisplacement = builder.attributeExpression(Constants.Style.Grid.ATT_X_DISPLACEMENT);
        Expression yDisplacement = builder.attributeExpression(Constants.Style.Grid.ATT_Y_DISPLACEMENT);
        Expression xAnchor = builder.attributeExpression(Constants.Style.Grid.ATT_ANCHOR_X);
        Expression yAnchor = builder.literalExpression(yAnchorPixels);
        Displacement displacement = builder.createDisplacement(xDisplacement, yDisplacement);
        Expression rotation = builder.attributeExpression(Constants.Style.Grid.ATT_ROTATION);

        AnchorPoint anchorPoint = builder.createAnchorPoint(xAnchor, yAnchor);
        PointPlacement text1Placement = builder.createPointPlacement(anchorPoint, displacement, rotation);
        final TextSymbolizer text1 = builder.createTextSymbolizer();
        text1.setFill(builder.createFill(color));
        text1.setLabelPlacement(text1Placement);
        final double opacity = 0.8;
        text1.setHalo(builder.createHalo(Color.white, opacity, 2));
        text1.setLabel(builder.attributeExpression(Constants.Style.Grid.ATT_LABEL));
        return text1;
    }

}
