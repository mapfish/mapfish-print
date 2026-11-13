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
    // The output stream belongs to MapFish Print and must remain open for its caller.
    assertFalse(
        outputStream.wasClosed(), "COG export must not close the MapFish-managed OutputStream");

    final byte[] result = outputStream.toByteArray();

    // The export must be nonempty, tiled, LZW-compressed, and correctly georeferenced.
    assertTrue(result.length > 0);
    assertTiffProperties(result);
    assertGeoreferencing(result);
  }

  /** Checks the exported TIFF's 512-pixel tiles and LZW compression. */
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

        // TIFF tag 259 identifies the compression algorithm; value 5 means LZW.
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

  /** Checks the geographic bounds requested by the unrotated bbox fixture. */
  private void assertGeoreferencing(final byte[] result) throws Exception {
    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader = geoTiffFormat.getReader(new ByteArrayInputStream(result));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();

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

  /**
   * Verifies that the center-and-scale request places the raster midpoint at the requested world
   * coordinates.
   */
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
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();
      final MathTransform gridToCRS = coverage.getGridGeometry().getGridToCRS();
      final double[] gridCenter = {
        (coverage.getRenderedImage().getWidth() - 1) / 2.0,
        (coverage.getRenderedImage().getHeight() - 1) / 2.0
      };
      final double[] worldCenter = new double[2];

      // For CELL_CENTER coordinates, the raster midpoint is (size - 1) / 2.
      gridToCRS.transform(gridCenter, 0, worldCenter, 0, 1);

      assertEquals(637395.386, worldCenter[0], 0.000001);
      assertEquals(5788326.345, worldCenter[1], 0.000001);

    } finally {
      reader.dispose();
    }
  }

  /**
   * Verifies that rotating a center-and-scale map preserves its center and rotates the pixel axes.
   */
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
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();
      final MathTransform gridToCRS = coverage.getGridGeometry().getGridToCRS();
      final double[] gridCenter = {
        (coverage.getRenderedImage().getWidth() - 1) / 2.0,
        (coverage.getRenderedImage().getHeight() - 1) / 2.0
      };
      final double[] worldCenter = new double[2];

      gridToCRS.transform(gridCenter, 0, worldCenter, 0, 1);

      // Rotation must not move the geographic center of the map.
      assertEquals(637395.386, worldCenter[0], 0.000001);
      assertEquals(5788326.345, worldCenter[1], 0.000001);

      // At 45 degrees, moving right by 100 pixels yields equal positive X/Y offsets.
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

  /**
   * Verifies the half-pixel offset between GeoTools CELL_CENTER and CELL_CORNER coordinates for
   * even and odd raster sizes.
   */
  @Test
  public void testGridToCrsPixelConvention() throws Exception {
    // GIVEN
    // Use even and odd rasters to verify the coordinate convention is size-independent.
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

      // WHEN
      // Interpret grid (0, 0) as both a pixel center and an outer pixel corner.
      final MathTransform centerTransform =
          coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER);

      final MathTransform cornerTransform =
          coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER);

      final double[] center = new double[2];
      final double[] corner = new double[2];

      centerTransform.transform(new double[] {0.0, 0.0}, 0, center, 0, 1);

      cornerTransform.transform(new double[] {0.0, 0.0}, 0, corner, 0, 1);

      // THEN
      // CELL_CORNER is shifted half a pixel left and up from CELL_CENTER.
      assertEquals(100.0, center[0], 1e-9);
      assertEquals(200.0, center[1], 1e-9);

      assertEquals(99.5, corner[0], 1e-9);
      assertEquals(200.5, corner[1], 1e-9);
    }
  }

  /**
   * Verifies that the geographic extent remains centered for all combinations of even and odd
   * raster dimensions.
   */
  @Test
  public void testGeoreferencingForEvenAndOddDimensions() throws Exception {
    // GIVEN
    // Cover every width/height parity combination, including mixed dimensions.
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

      // Pixel centers range from 0 to size - 1, so the midpoint is (size - 1) / 2.
      final double cx = (width - 1) / 2.0;
      final double cy = (height - 1) / 2.0;

      final AffineTransform transform = new AffineTransform();
      transform.translate(centerX, centerY);
      transform.scale(metersPerPixel, -metersPerPixel);
      transform.translate(-cx, -cy);

      // WHEN
      // Build a coverage using the pixel-center affine transformation.
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

      // THEN
      // The outer bounds are half the full raster width/height from the center,
      // regardless of whether a dimension is even or odd.
      final var envelope = coverage.getEnvelope2D();

      final String message = width + "x" + height;

      assertEquals(centerX - width / 2.0, envelope.getMinX(), 1e-9, message + " minX");

      assertEquals(centerX + width / 2.0, envelope.getMaxX(), 1e-9, message + " maxX");

      assertEquals(centerY - height / 2.0, envelope.getMinY(), 1e-9, message + " minY");

      assertEquals(centerY + height / 2.0, envelope.getMaxY(), 1e-9, message + " maxY");
    }
  }

  /**
   * Verifies that GeoTIFF writing and reading preserve the full geographic extent for even and odd
   * dimensions.
   */
  @Test
  public void testGeoTiffRoundTripGeoreferencing() throws Exception {
    // GIVEN
    // Test square and rectangular rasters with both pixel-dimension parities.
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
        // WHEN
        // Serialize the coverage to GeoTIFF and read it back.
        final GridCoverageWriter writer = format.getWriter(file);
        try {
          writer.write(coverage);
        } finally {
          writer.dispose();
        }

        final GridCoverageReader reader = format.getReader(file);

        try {
          final GridCoverage2D readCoverage = (GridCoverage2D) reader.read();

          // THEN
          // GeoTIFF serialization must preserve the geographic extent without
          // introducing a half-pixel shift in either dimension.
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

  /**
   * Verifies that DPI rounding changes raster dimensions without changing the intended geographic
   * extent.
   */
  @Test
  public void testCogExportGeoreferencingWithDpiRounding() throws Exception {
    // GIVEN
    // Render a 500x100 layout at 96 DPI instead of the base 72 DPI.
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center-dpi96.json");

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
    // The raster dimensions may round, but the geographic footprint must remain fixed.
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

  /**
   * Verifies that exported georeferencing uses the effective, snapped map scale instead of the
   * requested scale.
   */
  @Test
  public void testCogExportWithNearestScale() throws Exception {
    // GIVEN
    // A requested scale of 1:3600 is snapped to the configured 1:50000 level.
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
    // The exported extent must use the snapped scale rather than the requested scale.
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

  /**
   * Verifies that DPI rounding and nearest-scale snapping are applied consistently to the exported
   * raster.
   */
  @Test
  public void testCogExportWithDpiRoundingAndNearestScale() throws Exception {
    // GIVEN
    // Combine 96 DPI raster rounding with snapping from 1:3600 to 1:50000.
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center-dpi96-nearest.json");

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
    // Check both rounded raster dimensions and the footprint at the snapped scale.
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

  /** Verifies all four geographic corner coordinates of a rotated center-and-scale export. */
  @Test
  public void testCogExportRotatedCorners() throws Exception {
    // GIVEN
    // Export a center-and-scale map rotated 45 degrees around its center.
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
    // Check the outer corners; an axis-aligned envelope would not prove rotation.
    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();

      final int width = coverage.getRenderedImage().getWidth();
      final int height = coverage.getRenderedImage().getHeight();

      assertEquals(500, width);
      assertEquals(100, height);

      // The expected envelope is not enough to prove rotation: check the
      // four outer pixel corners with CELL_CORNER coordinates.
      final MathTransform gridToCRS =
          coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER);

      final double[][] gridCorners = {
        {0, 0},
        {width, 0},
        {width, height},
        {0, height}
      };

      // Independently calculated for a 500x100 raster,
      // 1.27 m per pixel, 45 degree rotation,
      // centered at (637395.386, 5788326.345).
      final double[][] expectedCorners = {
        {637125.978316368, 5788146.739877579},
        {637574.991122422, 5788595.752683632},
        {637664.793683632, 5788505.950122421},
        {637215.780877579, 5788056.937316367}
      };

      for (int i = 0; i < gridCorners.length; i++) {
        final double[] actual = new double[2];

        gridToCRS.transform(gridCorners[i], 0, actual, 0, 1);

        assertEquals(expectedCorners[i][0], actual[0], 1e-6, "Corner " + i + " X");

        assertEquals(expectedCorners[i][1], actual[1], 1e-6, "Corner " + i + " Y");
      }

    } finally {
      reader.dispose();
    }
  }

  /** Verifies that bbox aspect-ratio adjustment is reflected in the exported geographic bounds. */
  @Test
  public void testCogExportWithAdjustedBoundingBox() throws Exception {
    // GIVEN
    // Expand a requested 10x3 bbox to match the 500x100 raster aspect ratio.
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-bbox-adjusted.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // WHEN
    // Export using MapFish's aspect-ratio-adjusted map context.
    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    // THEN
    // The exported extent must reflect the adjusted, not the requested, bbox.
    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();

      assertEquals(500, coverage.getRenderedImage().getWidth());
      assertEquals(100, coverage.getRenderedImage().getHeight());

      // MapFish adjusts the original 10x3 bounding box
      // to the 5:1 aspect ratio of the rendered map.
      // The geographic center remains unchanged.
      final var envelope = coverage.getEnvelope2D();

      assertEquals(95.0, envelope.getMinX(), 1e-6);
      assertEquals(110.0, envelope.getMaxX(), 1e-6);
      assertEquals(-0.5, envelope.getMinY(), 1e-6);
      assertEquals(2.5, envelope.getMaxY(), 1e-6);

    } finally {
      reader.dispose();
    }
  }

  /** Verifies all four geographic corner coordinates of a rotated bbox-based export. */
  @Test
  public void testCogExportWithRotatedBoundingBox() throws Exception {
    // GIVEN
    // Export the original 10x2 bbox with a 45-degree rotation.
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-bbox-rotation.json");

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
    // The raster corners must retain the affine rotation for a bbox request.
    final GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
    final GridCoverageReader reader =
        geoTiffFormat.getReader(new ByteArrayInputStream(outputStream.toByteArray()));

    assertNotNull(reader);

    try {
      final GridCoverage2D coverage = (GridCoverage2D) reader.read();

      assertEquals(500, coverage.getRenderedImage().getWidth());
      assertEquals(100, coverage.getRenderedImage().getHeight());

      // Check outer pixel corners; an axis-aligned envelope would miss rotation.
      final MathTransform gridToCRS =
          coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER);

      final double[][] gridCorners = {
        {0, 0},
        {500, 0},
        {500, 100},
        {0, 100}
      };

      // Independent expected coordinates for a 45-degree rotation
      // around (102.5, 0.5), with pixel resolution 0.02.
      final double[][] expectedCorners = {
        {98.257359313, -2.328427125},
        {105.328427125, 4.742640687},
        {106.742640687, 3.328427125},
        {99.671572875, -3.742640687}
      };

      for (int i = 0; i < gridCorners.length; i++) {
        final double[] actual = new double[2];

        gridToCRS.transform(gridCorners[i], 0, actual, 0, 1);

        assertEquals(expectedCorners[i][0], actual[0], 1e-6, "Corner " + i + " X");

        assertEquals(expectedCorners[i][1], actual[1], 1e-6, "Corner " + i + " Y");
      }
    } finally {
      reader.dispose();
    }
  }
}
