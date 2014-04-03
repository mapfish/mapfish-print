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

package org.mapfish.print.map.geotools;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleBBoxGeoJsonTest;

import java.io.File;

/**
 * @author Jesse on 4/3/14.
 */
public class GeotiffLayerFactoryPluginTest {

    @Test(expected = IllegalArgumentException.class)
    public void testGeoIllegalFileUrl() throws Exception {
        final File file = AbstractMapfishSpringTest.
                getFile(CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest.class, CreateMapProcessorFlexibleScaleAndCenterGeoTiffTest
                                                                                           .BASE_DIR + "sampleGeoTiff.tif");
        final Configuration configuration = new Configuration();
        configuration.setConfigurationFile(File.createTempFile("xyz", ".yaml"));

        Template template = new Template();
        template.setConfiguration(configuration);
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));


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
        template.setStyle("polygon", template.getConfiguration().getDefaultStyle("polygon"));


        GeotiffLayer.GeotiffParam param = new GeotiffLayer.GeotiffParam();
        param.url = "file://../" + CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "/geojson.json";
        new GeotiffLayer.Plugin().parse(template, param);
    }
}
