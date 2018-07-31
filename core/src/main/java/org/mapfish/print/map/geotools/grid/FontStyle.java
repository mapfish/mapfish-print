package org.mapfish.print.map.geotools.grid;

import java.awt.Font;

/**
 * Enumeration of all the allowed font styles.
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

    final int styleId;

    FontStyle(final int styleId) {
        this.styleId = styleId;
    }
}
