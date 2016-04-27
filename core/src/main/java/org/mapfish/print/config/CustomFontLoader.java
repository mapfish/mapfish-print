package org.mapfish.print.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;


/**
 * Used to load custom fonts.
 *
 * To add a custom font, the file `mapfish-spring-custom-fonts.xml` must be overridden.
 */
public final class CustomFontLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFontLoader.class);

    /**
     * Load the custom fonts when the application is started.
     * @param paths A list of paths to ttf font files.
     */
    public CustomFontLoader(final Set<String> paths) {

        for (String path : paths) {
            try {
                loadFont(path);
            } catch (FontFormatException e) {
                throw new ConfigurationException("Font could not be created " + path, e);
            } catch (IOException e) {
                throw new ConfigurationException("Can not read font file " + path, e);
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
            LOGGER.warn(
                    "Font " + fontFile.getAbsolutePath() + " could not be registered. " +
                    "Is there already a system font with the same name?");
        } else {
            LOGGER.info("Font " + fontFile.getAbsolutePath() + " registered successfully");
        }
    }
}
