/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.test.util;

import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporterParameter;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

/**
 * Class for comparing an image to another image.
 *
 * @author Jesse on 3/27/14.
 *         <p/>
 *         CHECKSTYLE:OFF
 */
public final class ImageSimilarity {

    static final int DEFAULT_SAMPLESIZE = 15;
    // The reference image "signature" (25 representative pixels, each in R,G,B).
    // We use instances of Color to make things simpler.
    private final Color[][] signature;
    // The size of the sampling area.
    private int sampleSize = DEFAULT_SAMPLESIZE;
    // values that are used to generate the position of the sample pixels
    private final float[] prop = new float[]
            {1f / 10f, 3f / 10f, 5f / 10f, 7f / 10f, 9f / 10f};

    /**
     * The constructor, which creates the GUI and start the image processing task.
     */
    public ImageSimilarity(final File referenceImage, final int sampleSize) throws IOException {
        this(ImageIO.read(referenceImage), sampleSize);
    }

    /*
     * The constructor, which creates the GUI and start the image processing task.
     */
    public ImageSimilarity(BufferedImage referenceImage, int sampleSize) throws IOException {
        if (referenceImage.getWidth() * prop[0] - sampleSize < 0 ||
            referenceImage.getWidth() * prop[4] + sampleSize > referenceImage.getWidth()) {
            throw new IllegalArgumentException("sample size is too big for the image.");
        }
        if (referenceImage.getHeight() * prop[0] - sampleSize < 0 ||
            referenceImage.getHeight() * prop[4] + sampleSize > referenceImage.getHeight()) {
            throw new IllegalArgumentException("sample size is too big for the image.");
        }
        this.sampleSize = sampleSize;

        signature = calcSignature(referenceImage);
    }

    /*
     * This method calculates and returns signature vectors for the input image.
     */
    private Color[][] calcSignature(BufferedImage i) {
        // Get memory for the signature.
        Color[][] sig = new Color[5][5];
        // For each of the 25 signature values average the pixels around it.
        // Note that the coordinate of the central pixel is in proportions.
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                sig[x][y] = averageAround(i, prop[x], prop[y]);
            }
        }
        return sig;
    }

    /*
     * This method averages the pixel values around a central point and return the
     * average as an instance of Color. The point coordinates are proportional to
     * the image.
     */
    private Color averageAround(BufferedImage i, double px, double py) {
        // Get an iterator for the image.
        RandomIter iterator = RandomIterFactory.create(i, null);
        // Get memory for a pixel and for the accumulator.
        double[] pixel = new double[i.getSampleModel().getNumBands()];
        double[] accum = new double[3];
        int numPixels = 0;
        // Sample the pixels.

        for (double x = px * i.getWidth() - sampleSize; x < px * i.getWidth() + sampleSize; x++) {
            for (double y = py * i.getHeight() - sampleSize; y < py * i.getHeight() + sampleSize; y++) {
                iterator.getPixel((int) x, (int) y, pixel);
                accum[0] += pixel[0];
                accum[1] += pixel[1];
                accum[2] += pixel[2];
                numPixels++;
            }
        }
        // Average the accumulated values.
        accum[0] /= numPixels;
        accum[1] /= numPixels;
        accum[2] /= numPixels;
        return new Color((int) accum[0], (int) accum[1], (int) accum[2]);
    }

    /*
     * This method calculates the distance between the signatures of an image and
     * the reference one. The signatures for the image passed as the parameter are
     * calculated inside the method.
     */
    private double calcDistance(final BufferedImage other) {
        // Calculate the signature for that image.
        Color[][] sigOther = calcSignature(other);
        // There are several ways to calculate distances between two vectors,
        // we will calculate the sum of the distances between the RGB values of
        // pixels in the same positions.
        double dist = 0;
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                int r1 = this.signature[x][y].getRed();
                int g1 = this.signature[x][y].getGreen();
                int b1 = this.signature[x][y].getBlue();
                int r2 = sigOther[x][y].getRed();
                int g2 = sigOther[x][y].getGreen();
                int b2 = sigOther[x][y].getBlue();
                double tempDist = Math.sqrt((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2));
                dist += tempDist;
            }
        }
        return dist;
    }

    /**
     * Check that the other image and the image calculated by this object are within the given distance.
     *
     * @param other       the image to compare to "this" image.
     * @param maxDistance the maximum distance between the two images.
     */
    public void assertSimilarity(File other, double maxDistance) throws IOException {
        final double distance = calcDistance(ImageIO.read(other));
        if (distance > maxDistance) {
            throw new AssertionError("similarity difference between images is: " + distance +
                                     " which is greater than the max distance of " + maxDistance);
        }
    }

    /**
     * Write the image to a file in uncompressed tiff format.
     *
     * @param image image to write
     * @param file  path and file name (extension will be ignored and changed to tiff.
     */
    public static void writeUncompressedImage(BufferedImage image, String file) throws IOException {
        FileImageOutputStream out = null;
        try {
            final File parentFile = new File(file).getParentFile();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("tiff");
            final ImageWriter next = writers.next();

            final ImageWriteParam param = next.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_DISABLED);

            final File outputFile = new File(parentFile, Files.getNameWithoutExtension(file) + ".tiff");

            out = new FileImageOutputStream(outputFile);
            next.setOutput(out);
            next.write(image);
        } catch (Throwable e) {
            System.err.println("Error writing the image generated by the test:" + file + "\n\t");
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Merges a list of graphic files into a single graphic.
     *
     * @param graphicFiles a list of graphic files
     * @param width        the graphic width (required for svg files)
     * @param height       the graphic height (required for svg files)
     * @return a single graphic
     * @throws TranscoderException
     */
    public static BufferedImage mergeImages(List<URI> graphicFiles, int width, int height)
            throws IOException, TranscoderException {
        if (graphicFiles.size() == 0) {
            throw new IllegalArgumentException("no graphics given");
        }

        BufferedImage mergedImage = loadGraphic(graphicFiles.get(0), width, height);
        Graphics g = mergedImage.getGraphics();
        for (int i = 1; i < graphicFiles.size(); i++) {
            BufferedImage image = loadGraphic(graphicFiles.get(i), width, height);
            g.drawImage(image, 0, 0, null);
        }
        g.dispose();

        // ImageIO.write(mergedImage, "tiff", new File("/tmp/expectedSimpleImage.tiff"));

        return mergedImage;
    }

    private static BufferedImage loadGraphic(URI path, int width, int height) throws IOException, TranscoderException {
        File file = new File(path);

        if (file.getName().endsWith(".svg")) {
            return convertFromSvg(path, width, height);
        } else {
            return ImageIO.read(file);
        }
    }

    /**
     * Renders an SVG image into a {@link BufferedImage}.
     */
    public static BufferedImage convertFromSvg(URI svgFile, int width, int height) throws TranscoderException {
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
        public BufferedImage createImage(int w, int h) {
            BufferedImage bi = new BufferedImage(w, h,
                    BufferedImage.TYPE_INT_ARGB);
            return bi;
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            this.img = img;
        }

        public BufferedImage getBufferedImage() {
            return img;
        }
    }

    /**
     * Exports a rendered {@link JasperPrint} to a {@link BufferedImage}.
     */
    public static BufferedImage exportReportToImage(JasperPrint jasperPrint, Integer page) throws Exception {
        BufferedImage pageImage = new BufferedImage(jasperPrint.getPageWidth(), jasperPrint.getPageHeight(), BufferedImage.TYPE_INT_RGB);

        JRGraphics2DExporter exporter = new JRGraphics2DExporter();
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
        exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, pageImage.getGraphics());
        exporter.setParameter(JRExporterParameter.PAGE_INDEX, page);
        exporter.exportReport();

        return pageImage;
    }

    /**
     * Exports a rendered {@link JasperPrint} to a graphics file.
     */
    public static void exportReportToFile(JasperPrint jasperPrint, String fileName, Integer page) throws Exception {
        BufferedImage pageImage = exportReportToImage(jasperPrint, page);
        ImageIO.write(pageImage, Files.getFileExtension(fileName), new File(fileName));
    }

    public static void main(String args[]) throws IOException {
        final String path = "C:\\GitHub\\mapfish-printV3\\core\\src\\test\\resources\\map-data";
        final File root = new File(path);
        final FluentIterable<File> files = Files.fileTreeTraverser().postOrderTraversal(root);
        for (File file : files) {
            if (Files.getFileExtension(file.getName()).equals("png")) {
                final BufferedImage img = ImageIO.read(file);
                writeUncompressedImage(img, file.getAbsolutePath());
            }
        }
    }
}
