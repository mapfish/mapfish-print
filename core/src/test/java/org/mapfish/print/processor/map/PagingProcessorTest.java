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

package org.mapfish.print.processor.map;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import jsr166y.ForkJoinPool;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;

/**
 * Basic test of the Map processor.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class PagingProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "paging_processor_test/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private MapfishParser parser;
    @Autowired
    private Map<String, OutputFormat> outputFormat;
    @Autowired
    ForkJoinPool forkJoinPool;

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        final String host = "paging_processor_test";
        requestFactory.registerHandler(
                new Predicate<URI>() {
                    @Override
                    public boolean apply(URI input) {
                        return (("" + input.getHost()).contains(host + ".json")) || input.getAuthority().contains(host + ".json");
                    }
                }, new TestHttpClientFactory.Handler() {
                    @Override
                    public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                        try {
                            byte[] bytes = Files.toByteArray(getFile("/map-data" + uri.getPath()));
                            return ok(uri, bytes, httpMethod);
                        } catch (AssertionError e) {
                            return error404(uri, httpMethod);
                        }
                    }
                }
        );
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        JasperPrint print = format.getJasperPrint(requestData, config, config.getDirectory(), getTaskDirectory()).print;

        assertEquals(7, print.getPages().size());
        for (int i = 0; i < print.getPages().size(); i++) {
            BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, i);
            ImageIO.write(reportImage, "png", new File("e:/tmp/" + "expected-page-"+i+".png"));

//            File expectedImage = getFile(BASE_DIR + "output/expected-page-"+i+".png");
//            new ImageSimilarity(reportImage, 5).assertSimilarity(expectedImage, 10);
        }
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(PagingProcessorTest.class, BASE_DIR + "requestData.json");
    }
}
