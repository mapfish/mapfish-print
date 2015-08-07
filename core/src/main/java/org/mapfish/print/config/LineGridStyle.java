package org.mapfish.print.config;

import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;

import java.awt.Color;

/**
 * Creates the Named LineGridStyle.
 *
 * @author Jesse on 6/29/2015.
 */
public final class LineGridStyle {
    /**
     * Default GRID COLOR.
     */
    static final Color GRID_COLOR = Color.gray;

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
        final Color strokeColor = GRID_COLOR;
        lineSymbolizer.setStroke(builder.createStroke(strokeColor, 1, new float[]{4f, 4f}));
        //CSON:MagicNumber

        return builder.createStyle(lineSymbolizer);
    }

}
