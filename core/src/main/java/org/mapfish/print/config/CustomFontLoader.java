package org.mapfish.print.config;


import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontFamily;
import net.sf.jasperreports.engine.fonts.FontUtil;
import net.sf.jasperreports.extensions.ExtensionsEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;


/**
 * Used to load custom fonts.
 * <p>
 * To add a custom font, the file `mapfish-spring-custom-fonts.xml` must be overridden.
 * <p>
 * Warning: the fonts added using this are not available to Jasper Reports. They can only be used in layer
 * styles.
 */
public final class CustomFontLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFontLoader.class);

    /**
     * Load the custom fonts when the application is started.
     *
     * @param paths A list of paths to ttf font files.
     */
    public CustomFontLoader(final Set<String> paths) {

        for (String path: paths) {
            try {
                loadFont(path);
            } catch (FontFormatException e) {
                throw new ConfigurationException("Font could not be created " + path, e);
            } catch (IOException e) {
                throw new ConfigurationException("Can not read font file " + path, e);
            }
        }
        registerJasperFonts();
    }

    private void registerJasperFonts() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final List<FontFamily> families =
                ExtensionsEnvironment.getExtensionsRegistry().getExtensions(FontFamily.class);
        for (FontFamily family: families) {
            for (int style = 0; style <= 3; ++style) {
                final Font font = FontUtil.getInstance(DefaultJasperReportsContext.getInstance())
                        .getAwtFontFromBundles(family.getName(), style, 10.0f, null, true);
                if (font != null && ge.registerFont(font)) {
                    LOGGER.info("Font {} from Jasper registered successfully", font);
                }
            }
        }
    }

    private void loadFont(final String path) throws FontFormatException, IOException {
        URL url = CustomFontLoader.class.getClassLoader().getResource(path);
        if (url != null) {
            File fontFile = new File(url.getFile());
            if (fontFile.canRead()) {
                java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile);
                registerFont(font, fontFile);
            } else {
                throw new ConfigurationException("Can not read font file " + fontFile.getAbsolutePath());
            }
        } else {
            throw new ConfigurationException("Can not read font file " + path);
        }
    }

    private void registerFont(final java.awt.Font font, final File fontFile) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        boolean registered = ge.registerFont(font);
        if (!registered) {
            LOGGER.warn("Font {} could not be registered. Is there already a system font with the same name?",
                        fontFile.getAbsolutePath());
        } else {
            LOGGER.info("Font {} registered successfully", fontFile.getAbsolutePath());
        }
    }
}
