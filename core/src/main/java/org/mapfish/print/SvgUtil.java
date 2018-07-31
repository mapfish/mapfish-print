package org.mapfish.print;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;

import java.awt.image.BufferedImage;
import java.net.URI;

/**
 * SVG Utilities.
 */
public final class SvgUtil {

    private SvgUtil() {

    }

    /**
     * Renders an SVG image into a {@link BufferedImage}.
     *
     * @param svgFile the svg file
     * @param width the width
     * @param height the height
     * @return a buffered image
     * @throws TranscoderException
     */
    public static BufferedImage convertFromSvg(final URI svgFile, final int width, final int height)
            throws TranscoderException {
        BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();

        imageTranscoder.addTranscodingHint(TIFFTranscoder.KEY_WIDTH, (float) width);
        imageTranscoder.addTranscodingHint(TIFFTranscoder.KEY_HEIGHT, (float) height);

        TranscoderInput input = new TranscoderInput(svgFile.toString());
        imageTranscoder.transcode(input, null);

        return imageTranscoder.getBufferedImage();
    }

    /**
     * An image transcoder which allows to retrieve an {@link BufferedImage}.
     */
    private static class BufferedImageTranscoder extends ImageTranscoder {

        private BufferedImage img = null;

        @Override
        public BufferedImage createImage(final int w, final int h) {
            BufferedImage bi = new BufferedImage(w, h,
                                                 BufferedImage.TYPE_INT_ARGB);
            return bi;
        }

        @Override
        public void writeImage(final BufferedImage image, final TranscoderOutput output) {
            this.img = image;
        }

        public BufferedImage getBufferedImage() {
            return this.img;
        }
    }

}
