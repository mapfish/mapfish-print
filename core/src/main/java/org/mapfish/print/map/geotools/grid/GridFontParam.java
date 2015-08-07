package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.util.Assert;
import org.mapfish.print.parser.HasDefaultValue;

import java.awt.Font;
import javax.swing.JLabel;

/**
 * Configuration object for the grid labels.
 *
 * @author Jesse on 8/6/2015.
 * CSOFF: VisibilityModifier
 */
public final class GridFontParam {
    private static final Font DEFAULT_FONT_NAME = new JLabel().getFont();

    /**
     * The name of the font.
     */
    @HasDefaultValue
    public String name = DEFAULT_FONT_NAME.getFontName();
    /**
     * The size of the font. Default size is System default.
     */
    @HasDefaultValue
    public int size = DEFAULT_FONT_NAME.getSize();
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

    }
}
