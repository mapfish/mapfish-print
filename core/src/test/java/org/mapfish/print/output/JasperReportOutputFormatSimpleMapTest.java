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

package org.mapfish.print.output;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class JasperReportOutputFormatSimpleMapTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "simple_map/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    
    @Autowired
    private JasperReportPNGOutputFormat outputFormat;

    @Test
    public void testPrint() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData();
        
        JasperPrint print = outputFormat.getJasperPrint(requestData, config, 
                getFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR), getTaskDirectory());
        BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, 0);

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(reportImage, 50).assertSimilarity(getFile(BASE_DIR + "expectedReport.png"), 10);
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(JasperReportOutputFormatSimpleMapTest.class, BASE_DIR + "requestData.json");
    }

}
