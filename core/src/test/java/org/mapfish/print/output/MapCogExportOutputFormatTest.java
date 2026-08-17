package org.mapfish.print.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
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
