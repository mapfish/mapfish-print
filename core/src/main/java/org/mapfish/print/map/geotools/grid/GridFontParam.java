package org.mapfish.print.map.geotools.grid;

import org.locationtech.jts.util.Assert;
import org.mapfish.print.parser.HasDefaultValue;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.JLabel;

/**
 * Configuration object for the grid labels.
 */
public final class GridFontParam {
    private static final Font DEFAULT_FONT_NAME = new JLabel().getFont();
    private static final int DEFAULT_FONT_SIZE = 10;

    /**
     * The name of the font.
     */
    @HasDefaultValue
    public String[] name = {DEFAULT_FONT_NAME.getFontName()};
    /**
     * The size of the font. 10.
     */
    @HasDefaultValue
    public int size = DEFAULT_FONT_SIZE;
    /**
     * The style of the font.  Default BOLD
     */
    @HasDefaultValue
    public FontStyle style = FontStyle.BOLD;

    /**
     * Initialize default values and validate that config is correct.
     */
    public void postConstruct() {
        Assert.isTrue(this.name != null, "name parameter cannot be null");
        Assert.isTrue(this.style != null, "style parameter cannot be null");
        Assert.isTrue(this.size > 1, "size must be greater than 1");

        Font baseFont = null;
        for (String fontName: this.name) {
            try {
                baseFont = new Font(fontName, this.style.styleId, this.size);
                break;
            } catch (Exception e) {
                // try next font in list
            }
        }

        if (baseFont == null) {
            String[] legalFonts =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            throw new IllegalArgumentException(
                    Arrays.toString(this.name) + " does not contain a font that can be created by this Java "
                            + "Virtual Machine, legal options are: \n" + Arrays.toString(legalFonts));
        }
    }
}
