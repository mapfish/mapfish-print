package org.mapfish.print.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.coverage.grid.GridCoverageReader;
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
}
