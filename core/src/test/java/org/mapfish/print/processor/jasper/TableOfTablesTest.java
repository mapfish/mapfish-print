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

package org.mapfish.print.processor.jasper;

import jsr166y.ForkJoinPool;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue;

/**
 * @author Jesse on 4/10/2014.
 */
public class TableOfTablesTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "tablelist/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private MapfishParser parser;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;
    @Autowired
    private Map<String, OutputFormat> outputFormat;

    @Test
    public void testBasicTableProperties() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values(requestData, template, parser, getTaskDirectory(), this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final DataSourceAttributeValue datasource = values.getObject("datasource", DataSourceAttributeValue.class);

        assertEquals(2, datasource.attributesValues.length);
    }

    @Test
    public void testRenderTable() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        JasperPrint print = format.getJasperPrint(requestData, config, config.getDirectory(), getTaskDirectory()).print;

        assertEquals(1, print.getPages().size());
            BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, 0);

//            final File output = new File("e:/tmp/expected-page.png");
//            output.getParentFile().mkdirs();
//            ImageIO.write(reportImage, "png", output);

            File expectedImage = getFile(BASE_DIR + "expected-page.png");
            new ImageSimilarity(reportImage, 5).assertSimilarity(expectedImage, 10);

    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(TableOfTablesTest.class, BASE_DIR + "requestData.json");
    }
}
