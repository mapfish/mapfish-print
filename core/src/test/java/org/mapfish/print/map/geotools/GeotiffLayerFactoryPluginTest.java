package org.mapfish.print.map.geotools;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.IllegalFileAccessException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleBBoxGeoJsonTest;

import java.io.File;

public class GeotiffLayerFactoryPluginTest {

    @Test(expected = IllegalFileAccessException.class)
    public void testGeoIllegalFileUrl() throws Exception {
        final File file = AbstractMapfishSpringTest.
                getFile(CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest.class,
                        CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest
                                .BASE_DIR + "sampleGeoTiff.tif");
        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

        Template template = new Template();
        template.setConfiguration(configuration);

        GeotiffLayer.GeotiffParam param = new GeotiffLayer.GeotiffParam();
        param.url = file.toURI().toURL().toString();
        new GeotiffLayer.Plugin().parse(template, param);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoIllegalFileUrl2() throws Exception {
        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

        Template template = new Template();
        template.setConfiguration(configuration);

        GeotiffLayer.GeotiffParam param = new GeotiffLayer.GeotiffParam();
        param.url = "file://../" + CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "/geojson.json";
        new GeotiffLayer.Plugin().parse(template, param);
    }
}
