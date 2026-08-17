package org.mapfish.print.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class MapCogExportOutputFormatTest extends AbstractMapfishSpringTest {

  private static final String BASE_DIR = "map_cog/";

  @Autowired private ConfigurationFactory configurationFactory;

  @Autowired private Map<String, OutputFormat> outputFormat;

  @Test
  public void testCogExport() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(MapCogExportOutputFormatTest.class, BASE_DIR + "requestData.json");

    final OutputFormat format = this.outputFormat.get("cogMapOutputFormat");

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(MapCogExportOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    final byte[] result = outputStream.toByteArray();

    assertTrue(result.length > 0);

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
      final GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

      final MathTransform gridToCRS = coverage.getGridGeometry().getGridToCRS();

      final double[] gridCenter = {
        coverage.getRenderedImage().getWidth() / 2.0, coverage.getRenderedImage().getHeight() / 2.0
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
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    final PJsonObject requestData =
        parseJSONObjectFromFile(
            MapCogExportOutputFormatTest.class, BASE_DIR + "requestData-center-rotation.json");

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
      final GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

      final MathTransform gridToCRS = coverage.getGridGeometry().getGridToCRS();

      final double[] gridCenter = {
        coverage.getRenderedImage().getWidth() / 2.0, coverage.getRenderedImage().getHeight() / 2.0
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
}
