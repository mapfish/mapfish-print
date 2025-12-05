package org.mapfish.print.map.geotools;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.IllegalFileAccessException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleBBoxGeoJsonTest;

public class GeotiffLayerFactoryPluginTest {

  @Test
  public void testGeoIllegalFileUrl() throws Exception {
    assertThrows(IllegalFileAccessException.class, () -> {
      final File file =
          AbstractMapfishSpringTest.getFile(
              CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest.class,
              CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest.BASE_DIR + "sampleGeoTiff.tif");
      final Configuration configuration = new Configuration();
      configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

      Template template = new Template();
      template.setConfiguration(configuration);

      GeotiffLayer.GeotiffParam param = new GeotiffLayer.GeotiffParam();
      param.url = file.toURI().toURL().toString();
      new GeotiffLayer.Plugin().parse(template, param);
    });
  }

  @Test
  public void testGeoIllegalFileUrl2() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> {
      final Configuration configuration = new Configuration();
      configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

      Template template = new Template();
      template.setConfiguration(configuration);

      GeotiffLayer.GeotiffParam param = new GeotiffLayer.GeotiffParam();
      param.url =
          "file://../" + CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "/geojson.json";
      new GeotiffLayer.Plugin().parse(template, param);
    });
  }
}
