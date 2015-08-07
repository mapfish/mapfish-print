package org.mapfish.print.map.geotools.grid;

import java.awt.Font;

/**
 * Enumeration of all the allowed font styles.
 *
 * @author Jesse on 8/6/2015.
 */
public enum FontStyle {
    /**
     * The plain style.
     */
    PLAIN(Font.PLAIN),
    /**
     * The bold style.
     */
    BOLD(Font.BOLD),
    /**
     * The italic style.
     */
    ITALIC(Font.ITALIC);

    // CSOFF: VisibilityModifier
    final int styleId;
    // CSON: VisibilityModifier

    private FontStyle(final int styleId) {
        this.styleId = styleId;
    }
}
