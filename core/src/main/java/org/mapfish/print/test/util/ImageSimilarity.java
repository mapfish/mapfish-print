package org.mapfish.print.test.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleGraphics2DReportConfiguration;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.mapfish.print.SvgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for comparing an expected image to an actual image.
 *
 * <p>CHECKSTYLE:OFF
 */
public final class ImageSimilarity {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImageSimilarity.class);

  public static final boolean REGENERATE_EXPECTED_IMAGES = false;

  private final BufferedImage expectedImage;
  private final BufferedImage maskImage;
  private final BufferedImage diffImage;
  private final File expectedPath;

  /** The constructor, which creates the GUI and start the image processing task. */
  public ImageSimilarity(final File expectedFile) throws IOException {
    this.expectedImage = expectedFile.exists() ? ImageIO.read(expectedFile) : null;
    if (REGENERATE_EXPECTED_IMAGES || !expectedFile.exists()) {
      this.expectedPath =
          new File(
              expectedFile
                  .toString()
                  .replace("/out/", "/src/")
                  .replace("/build/classes/test/", "/src/test/resources/")
                  .replace("/build/resources/test/", "/src/test/resources/"));
    } else {
      this.expectedPath = expectedFile;
    }
    final File maskFile = getRelatedFile("mask");
    if (maskFile.exists()) {
      this.maskImage = ImageIO.read(maskFile);
      assert this.maskImage.getSampleModel().getNumBands() == 1;
    } else if (this.expectedImage != null) {
      this.maskImage =
          new BufferedImage(
              this.expectedImage.getWidth(),
              this.expectedImage.getHeight(),
              BufferedImage.TYPE_BYTE_GRAY);

      final Graphics2D graphics = this.maskImage.createGraphics();
      try {
        graphics.setBackground(new Color(255, 255, 255));
        graphics.clearRect(0, 0, this.expectedImage.getWidth(), this.expectedImage.getHeight());
      } finally {
        graphics.dispose();
      }
    } else {
      this.maskImage = null;
    }
    if (this.expectedImage != null) {
      this.diffImage =
          new BufferedImage(
              this.expectedImage.getWidth(),
              this.expectedImage.getHeight(),
              BufferedImage.TYPE_INT_RGB);
    } else {
      this.diffImage = null;
    }
  }

  /**
   * Write the image to a file in uncompressed tiff format.
   *
   * @param image image to write
   * @param file path and file name (extension will be ignored and changed to tiff.
   * @throws IOException if the image could not be written.
   */
  private static void writeUncompressedImage(BufferedImage image, String file) throws IOException {
    try {
      final File parentFile = new File(file).getParentFile();
      Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("png");
      final ImageWriter next = writers.next();

      final ImageWriteParam param = next.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_DISABLED);

      final File outputFile = new File(parentFile, FilenameUtils.getBaseName(file) + ".png");

      try (FileImageOutputStream out = new FileImageOutputStream(outputFile)) {
        next.setOutput(out);
        next.write(image);
      }
    } catch (Throwable e) {
      System.err.println(
          String.format("Error writing the image generated by the test: %s%n\t", file));
      e.printStackTrace();
    }
  }

  /**
   * Merges a list of graphic files into a single graphic.
   *
   * @param graphicFiles a list of graphic files
   * @param width the graphic width (required for svg files)
   * @param height the graphic height (required for svg files)
   * @return a single graphic
   */
  public static BufferedImage mergeImages(List<URI> graphicFiles, int width, int height)
      throws IOException, TranscoderException {
    if (graphicFiles.isEmpty()) {
      throw new IllegalArgumentException("no graphics given");
    }

    BufferedImage mergedImage = loadGraphic(graphicFiles.get(0), width, height);
    Graphics g = mergedImage.getGraphics();
    for (int i = 1; i < graphicFiles.size(); i++) {
      BufferedImage image = loadGraphic(graphicFiles.get(i), width, height);
      g.drawImage(image, 0, 0, null);
    }
    g.dispose();

    return mergedImage;
  }

  private static BufferedImage loadGraphic(final URI path, final int width, final int height)
      throws IOException, TranscoderException {
    File file = new File(path);

    if (file.getName().endsWith(".svg")) {
      return convertFromSvg(path, width, height);
    } else {
      BufferedImage originalImage = ImageIO.read(file);
      BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
      Graphics2D g = resizedImage.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.drawImage(originalImage, 0, 0, width, height, null);
      g.dispose();
      return resizedImage;
    }
  }

  /** Renders an SVG image into a {@link BufferedImage}. */
  public static BufferedImage convertFromSvg(final URI svgFile, final int width, final int height)
      throws TranscoderException {
    return SvgUtil.convertFromSvg(svgFile, width, height);
  }

  /** Exports a rendered {@link JasperPrint} to a {@link BufferedImage}. */
  public static BufferedImage exportReportToImage(final JasperPrint jasperPrint, final Integer page)
      throws JRException {
    BufferedImage pageImage =
        new BufferedImage(
            jasperPrint.getPageWidth(), jasperPrint.getPageHeight(), BufferedImage.TYPE_INT_RGB);

    JRGraphics2DExporter exporter = new JRGraphics2DExporter();

    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

    SimpleGraphics2DExporterOutput output = new SimpleGraphics2DExporterOutput();
    output.setGraphics2D((Graphics2D) pageImage.getGraphics());
    exporter.setExporterOutput(output);

    SimpleGraphics2DReportConfiguration configuration = new SimpleGraphics2DReportConfiguration();
    configuration.setPageIndex(page);
    exporter.setConfiguration(configuration);

    exporter.exportReport();

    return pageImage;
  }

  public static void main(final String args[]) throws IOException {
    final String path = "core/src/test/resources/map-data";
    final File root = new File(path);
    final Iterable<File> files = FileUtils.listFiles(root, new String[] {"png"}, true);
    for (File file : files) {
      final BufferedImage img = ImageIO.read(file);
      writeUncompressedImage(img, file.getAbsolutePath());
    }
  }

  private File getRelatedFile(final String name) {
    final String expectedFileName = this.expectedPath.getName();
    return new File(
        this.expectedPath.getParentFile(),
        (expectedFileName.contains("expected")
            ? expectedFileName.replace("expected", name)
            : name + "-" + expectedFileName));
  }

  /**
   * This method calculates the distance between the signatures of an image and the reference one.
   * The signatures for the image passed as the parameter are calculated inside the method.
   *
   * @return a number between 0 and 10000 or Double.MAX_VALUE on images format error.
   */
  double calcDistance(final BufferedImage actual) {
    // There are several ways to calculate distances between two vectors,
    // we will calculate the sum of the distances between the RGB values of
    // pixels in the same positions.
    if (actual.getWidth() != this.expectedImage.getWidth()) {
      LOGGER.error(
          "Not the same width (expected: {}, actual: {})",
          this.expectedImage.getWidth(),
          actual.getWidth());
      return Double.MAX_VALUE;
    }
    if (actual.getHeight() != this.expectedImage.getHeight()) {
      LOGGER.error(
          "Not the same height (expected: {}, actual: {})",
          this.expectedImage.getHeight(),
          actual.getHeight());
      return Double.MAX_VALUE;
    }
    if (actual.getSampleModel().getNumBands()
        != this.expectedImage.getSampleModel().getNumBands()) {
      LOGGER.error(
          "Not the same number of bands (expected: {}, actual: {})",
          this.expectedImage.getSampleModel().getNumBands(),
          actual.getSampleModel().getNumBands());
      return Double.MAX_VALUE;
    }
    double dist = 0;
    double[] expectedPixel = new double[this.expectedImage.getSampleModel().getNumBands()];
    double[] actualPixel = new double[this.expectedImage.getSampleModel().getNumBands()];
    int[] maskPixel = new int[1];
    RandomIter expectedIterator = RandomIterFactory.create(this.expectedImage, null);
    RandomIter actualIterator = RandomIterFactory.create(actual, null);
    RandomIter maskIterator = RandomIterFactory.create(this.maskImage, null);
    Graphics2D diffGraphics = this.diffImage.createGraphics();
    for (int x = 0; x < actual.getWidth(); x++) {
      for (int y = 0; y < actual.getHeight(); y++) {
        expectedIterator.getPixel(x, y, expectedPixel);
        actualIterator.getPixel(x, y, actualPixel);
        maskIterator.getPixel(x, y, maskPixel);
        double squareDist = 0.0;
        if (maskPixel[0] > 127) {
          for (int i = 0; i < this.expectedImage.getSampleModel().getNumBands(); i++) {
            double colorDist = expectedPixel[i] - actualPixel[i];
            squareDist += colorDist * colorDist;
          }
        }
        double pxDiff = Math.sqrt(squareDist / this.expectedImage.getSampleModel().getNumBands());
        dist += pxDiff / 255;
        diffGraphics.setColor(new Color((int) Math.round(pxDiff), 0, 0));
        diffGraphics.drawRect(x, y, 1, 1);
      }
    }
    diffGraphics.dispose();
    // Normalize
    dist = dist / this.expectedImage.getWidth() / this.expectedImage.getHeight() * 10000;
    LOGGER.debug("Current distance: {}", dist);
    return dist;
  }

  /**
   * Check that the actual image and the image calculated by this object are within the given
   * distance.
   *
   * @param actual the image to compare to "this" image.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(final File actual) throws IOException {
    assertSimilarity(actual, 1);
  }

  /**
   * Check that the actual image and the image calculated by this object are within the given
   * distance.
   *
   * @param maxDistance the maximum distance between the two images.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(final byte[] graphicData, final double maxDistance)
      throws IOException {
    assertSimilarity(ImageIO.read(new ByteArrayInputStream(graphicData)), maxDistance);
  }

  /**
   * Check that the actual image and the image calculated by this object are within the given
   * distance.
   *
   * @param graphicFiles a list of graphic files
   * @param width the graphic width (required for svg files)
   * @param height the graphic height (required for svg files)
   * @param maxDistance the maximum distance between the two images.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(
      final List<URI> graphicFiles, final int width, final int height, final double maxDistance)
      throws IOException, TranscoderException {
    assertSimilarity(mergeImages(graphicFiles, width, height), maxDistance);
  }

  /**
   * Check that the actual image and the image calculated by this object are within the given
   * distance.
   *
   * @param maxDistance the maximum distance between the two images.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(
      final URI svgFile, final int width, final int height, final double maxDistance)
      throws IOException, TranscoderException {
    assertSimilarity(convertFromSvg(svgFile, width, height), maxDistance);
  }

  /**
   * Check that the actual image and the image calculated by this object are within the given
   * distance.
   *
   * @param maxDistance the maximum distance between the two images.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(
      final JasperPrint jasperPrint, final Integer page, final double maxDistance)
      throws IOException, JRException {
    assertSimilarity(exportReportToImage(jasperPrint, page), maxDistance);
  }

  /**
   * Check that the actual image and the image calculated by this object are within the given
   * distance.
   *
   * @param actualFile the file to compare to "this" image.
   * @param maxDistance the maximum distance between the two images.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(final File actualFile, final double maxDistance) throws IOException {
    assertSimilarity(ImageIO.read(actualFile), maxDistance);
  }

  /**
   * Check that the actual image and the image calculated by this object are within a relay small
   * distance.
   *
   * @param actualImage the image to compare to "this" image.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(final BufferedImage actualImage) throws IOException {
    assertSimilarity(actualImage, 0);
  }

  /**
   * Check that the actual image and the image calculated by this object are within the given
   * distance.
   *
   * @param actualImage the image to compare to "this" image.
   * @param maxDistance the maximum distance between the two images.
   * @throws IOException if the image could not be written.
   */
  public void assertSimilarity(final BufferedImage actualImage, final double maxDistance)
      throws IOException {
    if (REGENERATE_EXPECTED_IMAGES || !this.expectedPath.exists()) {
      System.out.println("The expected file has been generated: " + expectedPath.getAbsolutePath());
      ImageIO.write(actualImage, "png", expectedPath);
      if (REGENERATE_EXPECTED_IMAGES) {
        return;
      } else {
        throw new AssertionError(
            "The expected file was missing and has been generated: "
                + expectedPath.getAbsolutePath());
      }
    }
    final double distance = calcDistance(actualImage);
    if (distance > maxDistance) {
      final File actualOutput = getRelatedFile("actual");
      ImageIO.write(actualImage, "png", actualOutput);
      final File diffOutput = getRelatedFile("diff");
      ImageIO.write(this.diffImage, "png", diffOutput);
      throw new AssertionError(
          String.format(
              "similarity difference between images is: %s which is "
                  + "greater than the max distance of %s%n"
                  + "actual=%s%n"
                  + "expected=%s",
              distance,
              maxDistance,
              actualOutput.getAbsolutePath(),
              this.expectedPath.getAbsolutePath()));
    }
  }
}
