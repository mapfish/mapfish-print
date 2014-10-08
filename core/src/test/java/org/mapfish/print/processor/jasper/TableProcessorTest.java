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

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nullable;

import jsr166y.ForkJoinPool;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import com.google.common.base.Predicate;
import com.google.common.io.Resources;

/**
 * @author Jesse on 4/10/2014.
 */
public class TableProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASIC_BASE_DIR = "table/";
    public static final String DYNAMIC_BASE_DIR = "table-dynamic/";
    public static final String IMAGE_CONVERTER_BASE_DIR = "table-image-column-resolver/";
    public static final String TABLE_CONVERTERS = "table_converters/";
    public static final String TABLE_CONVERTERS_DYNAMIC = "table_converters_dyn/";

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
        final String baseDir = BASIC_BASE_DIR;

        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData(baseDir);
        Values values = new Values(requestData, template, parser, getTaskDirectory(), this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final JRMapCollectionDataSource tableDataSource = values.getObject("table", JRMapCollectionDataSource.class);

        int count = 0;
        while (tableDataSource.next()) {
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void testBasicTablePrint() throws Exception {
        final String baseDir = BASIC_BASE_DIR;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print = format.getJasperPrint(requestData, config, file, getTaskDirectory()).print;
        BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, 0);

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(reportImage, 50).assertSimilarity(getFile(baseDir + "expectedImage.png"), 10);
    }

    @Test
    public void testDynamicTablePrint() throws Exception {
        final String baseDir = DYNAMIC_BASE_DIR;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print = format.getJasperPrint(requestData, config, file, getTaskDirectory()).print;
        BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, 0);

//        ImageIO.write(reportImage, "png", new File("e:/tmp/expectedImage.png"));

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(reportImage, 50).assertSimilarity(getFile(baseDir + "expectedImage.png"), 10);
    }

    @Test
    public void testColumnImageConverter() throws Exception {
        httpRequestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input.toString().contains("icons.com");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                final URL imageUrl = TableProcessorTest.class.getResource("/icons" + uri.getPath());
                final byte[] imageBytes = Resources.toByteArray(imageUrl);
                MockClientHttpRequest request = new MockClientHttpRequest();
                request.setResponse(new MockClientHttpResponse(imageBytes, HttpStatus.OK));
                return request;
            }
        });

        final String baseDir = IMAGE_CONVERTER_BASE_DIR;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print = format.getJasperPrint(requestData, config, file, getTaskDirectory()).print;
        BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, 0);
//        ImageIO.write(reportImage, "png", new File("e:/tmp/testColumnImageConverter.png"));
        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(reportImage, 50).assertSimilarity(getFile(baseDir + "expectedImage.png"), 10);
    }

    @Test
    public void testTableConverters() throws Exception {
        httpRequestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input.toString().contains("icons.com");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                final URL imageUrl = TableProcessorTest.class.getResource("/icons" + uri.getPath());
                final byte[] imageBytes = Resources.toByteArray(imageUrl);
                MockClientHttpRequest request = new MockClientHttpRequest();
                request.setResponse(new MockClientHttpResponse(imageBytes, HttpStatus.OK));
                return request;
            }
        });

        final String baseDir = TABLE_CONVERTERS;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print = format.getJasperPrint(requestData, config, file, getTaskDirectory()).print;
        BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, 0);
//        ImageIO.write(reportImage, "png", new File("/tmp/testColumnImageConverter.png"));
        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(reportImage, 50).assertSimilarity(getFile(baseDir + "expectedImage.png"), 10);
    }

    @Test
    public void testTableConvertersDynamic() throws Exception {
        final String baseDir = TABLE_CONVERTERS_DYNAMIC;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print = format.getJasperPrint(requestData, config, file, getTaskDirectory()).print;
        BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, 0);
//        ImageIO.write(reportImage, "png", new File("/tmp/expectedImage.png"));
        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(reportImage, 50).assertSimilarity(getFile(baseDir + "expectedImage.png"), 10);
    }

    private static PJsonObject loadJsonRequestData(String baseDir) throws IOException {
        return parseJSONObjectFromFile(TableProcessorTest.class, baseDir + "requestData.json");
    }
}