package org.mapfish.print.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.referencing.datum.PixelInCell;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class MapCogExportOutputFormatTest extends AbstractMapfishSpringTest {

  /**
   * Tracks whether the output stream is closed during export. The stream is managed by MapFish
   * Print and must remain open after the export has completed.
   */
  private static class TrackingOutputStream extends ByteArrayOutputStream {

    private boolean closed;

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }

    boolean wasClosed() {
      return closed;
    }
  }

  private static final String BASE_DIR = "map_cog/";

  @Autowired private ConfigurationFactory configurationFactory;

  @Autowired private Map<String, OutputFormat> outputFormat;

  /**
   * Verifies that the COG export produces a valid GeoTIFF with the expected properties while
   * leaving the MapFish-managed output stream open.
   */
  @Test
  public void testCogExport() throws Exception {
    // GIVEN
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(MapCogExportOutputFormatTest.class, BASE_DIR + "requestData.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");
    final TrackingOutputStream outputStream = new TrackingOutputStream();

    // WHEN
    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    // THEN

    // The output stream belongs to MapFish Print and must not be closed by the
    // exporter.
    assertFalse(
        outputStream.wasClosed(), "COG export must not close the MapFish-managed OutputStream");

    final byte[] result = outputStream.toByteArray();

    assertTrue(result.length > 0);
    assertTiffProperties(result);
    assertGeoreferencing(result);
  }

  private void assertTiffProperties(final byte[] result) throws Exception {
    final File tempFile = File.createTempFile("mapfish-cog-", ".tif");
    tempFile.deleteOnExit();

    Files.write(tempFile.toPath(), result);

    try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(tempFile)) {
      final Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");

      assertTrue(readers.hasNext());

      final ImageReader imageReader = readers.next();

      try {
        imageReader.setInput(imageInputStream);

        assertTrue(imageReader.isImageTiled(0));
        assertEquals(512, imageReader.getTileWidth(0));
        assertEquals(512, imageReader.getTileHeight(0));

        final IIOMetadata metadata = imageReader.getImageMetadata(0);
        assertNotNull(metadata);

        final String nativeFormatName = metadata.getNativeMetadataFormatName();
        assertNotNull(nativeFormatName);

        final var root = metadata.getAsTree(nativeFormatName);
        final var fields = ((org.w3c.dom.Node) root).getChildNodes();

        boolean lzwFound = false;

        for (int i = 0; i < fields.getLength(); i++) {
          final org.w3c.dom.Node node = fields.item(i);

          if ("TIFFIFD".equals(node.getNodeName())) {
            final var children = node.getChildNodes();

            for (int j = 0; j < children.getLength(); j++) {
              final org.w3c.dom.Node field = children.item(j);

              if ("TIFFField".equals(field.getNodeName())
                  && field.getAttributes() != null
                  && field.getAttributes().getNamedItem("number") != null
                  && "259".equals(field.getAttributes().getNamedItem("number").getNodeValue())) {

                final var compressionValues = field.getChildNodes();

                for (int k = 0; k < compressionValues.getLength(); k++) {
                  final org.w3c.dom.Node valueContainer = compressionValues.item(k);
                  final var values = valueContainer.getChildNodes();

                  for (int l = 0; l < values.getLength(); l++) {
                    final org.w3c.dom.Node value = values.item(l);

                    if (value.getAttributes() != null
                        && value.getAttributes().getNamedItem("value") != null
                        && "5".equals(value.getAttributes().getNamedItem("value").getNodeValue())) {
                      lzwFound = true;
                    }
                  }
                }
              }
            }
          }
        }

        assertTrue(lzwFound);
      } finally {
        imageReader.dispose();
      }
    }
  }

  private void assertGeoreferencing(final byte[] result) throws Exception {
    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader = geoTiffFormat.getReader(new ByteArrayInputStream(result));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

      assertNotNull(coverage);
      assertNotNull(coverage.getCoordinateReferenceSystem());

      final var envelope = coverage.getEnvelope2D();

      assertEquals(97.5, envelope.getMinX(), 0.000001);
      assertEquals(-0.5, envelope.getMinY(), 0.000001);
      assertEquals(107.5, envelope.getMaxX(), 0.000001);
      assertEquals(1.5, envelope.getMaxY(), 0.000001);
    } finally {
      reader.dispose();
    }
  }

  @Test
  public void testCogExportWithCenterAndScale() throws Exception {
    // GIVEN
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // WHEN
    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    // THEN
    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
      final MathTransform gridToCRS = coverage.getGridGeometry().getGridToCRS();
      final double[] gridCenter = {
        (coverage.getRenderedImage().getWidth() - 1) / 2.0,
        (coverage.getRenderedImage().getHeight() - 1) / 2.0
      };
      final double[] worldCenter = new double[2];

      gridToCRS.transform(gridCenter, 0, worldCenter, 0, 1);

      assertEquals(637395.386, worldCenter[0], 0.000001);
      assertEquals(5788326.345, worldCenter[1], 0.000001);

    } finally {
      reader.dispose();
    }
  }

  @Test
  public void testCogExportWithRotation() throws Exception {
    // GIVEN
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center-rotation.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // WHEN
    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    // THEN
    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
      final MathTransform gridToCRS = coverage.getGridGeometry().getGridToCRS();
      final double[] gridCenter = {
        (coverage.getRenderedImage().getWidth() - 1) / 2.0,
        (coverage.getRenderedImage().getHeight() - 1) / 2.0
      };
      final double[] worldCenter = new double[2];

      gridToCRS.transform(gridCenter, 0, worldCenter, 0, 1);

      assertEquals(637395.386, worldCenter[0], 0.000001);
      assertEquals(5788326.345, worldCenter[1], 0.000001);

      final double[] gridRight = {gridCenter[0] + 100.0, gridCenter[1]};
      final double[] worldRight = new double[2];

      gridToCRS.transform(gridRight, 0, worldRight, 0, 1);

      final double expectedDistance = 100.0 * 3600.0 * 0.0254 / 72.0;
      final double expectedOffset = expectedDistance / Math.sqrt(2.0);

      assertEquals(expectedOffset, worldRight[0] - worldCenter[0], 0.000001);
      assertEquals(expectedOffset, worldRight[1] - worldCenter[1], 0.000001);

    } finally {
      reader.dispose();
    }
  }

  @Test
  public void testGridToCrsPixelConvention() throws Exception {
    for (int size : new int[] {4, 5}) {
      final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

      // One world unit per pixel, with the first pixel center at (100, 200).
      final AffineTransform transform = new AffineTransform();
      transform.translate(100.0, 200.0);
      transform.scale(1.0, -1.0);

      final GridCoverage2D coverage =
          new GridCoverageFactory()
              .create(
                  "test",
                  image,
                  CRS.decode("EPSG:3857"),
                  new AffineTransform2D(transform),
                  null,
                  null,
                  null);

      final MathTransform centerTransform =
          coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER);

      final MathTransform cornerTransform =
          coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER);

      final double[] center = new double[2];
      final double[] corner = new double[2];

      centerTransform.transform(new double[] {0.0, 0.0}, 0, center, 0, 1);

      cornerTransform.transform(new double[] {0.0, 0.0}, 0, corner, 0, 1);

      assertEquals(100.0, center[0], 1e-9);
      assertEquals(200.0, center[1], 1e-9);

      assertEquals(99.5, corner[0], 1e-9);
      assertEquals(200.5, corner[1], 1e-9);
    }
  }

  @Test
  public void testGeoreferencingForEvenAndOddDimensions() throws Exception {
    final int[][] dimensions = {
      {4, 4},
      {5, 5},
      {4, 5},
      {5, 4}
    };

    for (final int[] dimension : dimensions) {
      final int width = dimension[0];
      final int height = dimension[1];

      final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

      final double centerX = 100.0;
      final double centerY = 200.0;
      final double metersPerPixel = 1.0;

      final double cx = (width - 1) / 2.0;
      final double cy = (height - 1) / 2.0;

      final AffineTransform transform = new AffineTransform();
      transform.translate(centerX, centerY);
      transform.scale(metersPerPixel, -metersPerPixel);
      transform.translate(-cx, -cy);

      final GridCoverage2D coverage =
          new GridCoverageFactory()
              .create(
                  "test",
                  image,
                  CRS.decode("EPSG:3857"),
                  new AffineTransform2D(transform),
                  null,
                  null,
                  null);

      final var envelope = coverage.getEnvelope2D();

      final String message = width + "x" + height;

      assertEquals(centerX - width / 2.0, envelope.getMinX(), 1e-9, message + " minX");

      assertEquals(centerX + width / 2.0, envelope.getMaxX(), 1e-9, message + " maxX");

      assertEquals(centerY - height / 2.0, envelope.getMinY(), 1e-9, message + " minY");

      assertEquals(centerY + height / 2.0, envelope.getMaxY(), 1e-9, message + " maxY");
    }
  }

  @Test
  public void testGeoTiffRoundTripGeoreferencing() throws Exception {
    final GeoTiffFormat format = new GeoTiffFormat();

    final int[][] dimensions = {
      {4, 4},
      {5, 5},
      {4, 5},
      {5, 4}
    };

    for (final int[] dimension : dimensions) {
      final int width = dimension[0];
      final int height = dimension[1];

      final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

      final double centerX = 100.0;
      final double centerY = 200.0;
      final double metersPerPixel = 1.0;

      final AffineTransform transform = new AffineTransform();
      transform.translate(centerX, centerY);
      transform.scale(metersPerPixel, -metersPerPixel);
      transform.translate(-(width - 1) / 2.0, -(height - 1) / 2.0);

      final GridCoverage2D coverage =
          new GridCoverageFactory()
              .create(
                  "test",
                  image,
                  CRS.decode("EPSG:3857"),
                  new AffineTransform2D(transform),
                  null,
                  null,
                  null);

      final File file = File.createTempFile("georeference-", ".tif");

      try {
        final GridCoverageWriter writer = format.getWriter(file);
        try {
          writer.write(coverage);
        } finally {
          writer.dispose();
        }

        final GridCoverageReader reader = format.getReader(file);

        try {
          final GridCoverage2D readCoverage = (GridCoverage2D) reader.read();

          final var envelope = readCoverage.getEnvelope2D();
          final String message = width + "x" + height;

          assertEquals(centerX - width / 2.0, envelope.getMinX(), 1e-7, message + " minX");

          assertEquals(centerX + width / 2.0, envelope.getMaxX(), 1e-7, message + " maxX");

          assertEquals(centerY - height / 2.0, envelope.getMinY(), 1e-7, message + " minY");

          assertEquals(centerY + height / 2.0, envelope.getMaxY(), 1e-7, message + " maxY");

        } finally {
          reader.dispose();
        }
      } finally {
        coverage.dispose(true);
        Files.deleteIfExists(file.toPath());
      }
    }
  }

  @Test
  public void testCogExportGeoreferencingWithDpiRounding() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center-dpi96.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();

      // Raster dimensions after DPI scaling and rounding.
      assertEquals(667, coverage.getRenderedImage().getWidth());
      assertEquals(134, coverage.getRenderedImage().getHeight());

      // Expected bounds from the original 500x100 map at
      // scale 1:3600 and PDF_DPI 72 (1.27 m per original pixel).
      // Width = 635 m, height = 127 m.
      final var envelope = coverage.getEnvelope2D();

      assertEquals(637077.886, envelope.getMinX(), 1e-6);
      assertEquals(637712.886, envelope.getMaxX(), 1e-6);

      assertEquals(5788262.845, envelope.getMinY(), 1e-6);
      assertEquals(5788389.845, envelope.getMaxY(), 1e-6);

    } finally {
      reader.dispose();
    }
  }

  @Test
  public void testCogExportWithNearestScale() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();

      assertEquals(500, coverage.getRenderedImage().getWidth());
      assertEquals(100, coverage.getRenderedImage().getHeight());

      // Requested scale: 1:3600.
      // Effective scale after snapping: 1:50000.
      final var envelope = coverage.getEnvelope2D();

      assertEquals(632985.6637777778, envelope.getMinX(), 1e-6);
      assertEquals(641805.1082222223, envelope.getMaxX(), 1e-6);
      assertEquals(5787444.400555556, envelope.getMinY(), 1e-6);
      assertEquals(5789208.289444444, envelope.getMaxY(), 1e-6);

    } finally {
      reader.dispose();
    }
  }

  @Test
  public void testCogExportWithDpiRoundingAndNearestScale() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center-dpi96-nearest.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();

      // Actual raster dimensions after scaling from 72 to 96 DPI.
      assertEquals(667, coverage.getRenderedImage().getWidth());
      assertEquals(134, coverage.getRenderedImage().getHeight());

      // Geographic extent at the effective scale 1:50000,
      // calculated from the original 500x100 map at 72 DPI.
      final var envelope = coverage.getEnvelope2D();

      assertEquals(632985.6637777778, envelope.getMinX(), 1e-6);
      assertEquals(641805.1082222223, envelope.getMaxX(), 1e-6);
      assertEquals(5787444.400555556, envelope.getMinY(), 1e-6);
      assertEquals(5789208.289444444, envelope.getMaxY(), 1e-6);

    } finally {
      reader.dispose();
    }
  }
}
