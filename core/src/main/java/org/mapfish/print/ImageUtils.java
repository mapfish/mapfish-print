package org.mapfish.print;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 * Utility functions for images.
 */
public final class ImageUtils {
    private ImageUtils() {
        // intentionally empty
    }

    /**
     * Equivalent to {@link ImageIO#write}, but handle errors.
     *
     * @param im a <code>RenderedImage</code> to be written.
     * @param formatName a <code>String</code> containing the informal name of the format.
     * @param output a <code>File</code> to be written to.
     * @throws IOException if an error occurs during writing.
     */
    public static void writeImage(final BufferedImage im, final String formatName, final File output)
            throws IOException {
        if (!ImageIO.write(im, formatName, output)) {
            throw new RuntimeException("Image format not supported: " + formatName);
        }
    }

    /**
     * Equivalent to {@link ImageIO#write}, but handle errors.
     *
     * @param im a <code>RenderedImage</code> to be written.
     * @param formatName a <code>String</code> containing the informal name of the format.
     * @param output a <code>File</code> to be written to.
     * @throws IOException if an error occurs during writing.
     */
    public static void writeImage(final BufferedImage im, final String formatName, final OutputStream output)
            throws IOException {
        if (!ImageIO.write(im, formatName, output)) {
            throw new RuntimeException("Image format not supported: " + formatName);
        }
    }
}
