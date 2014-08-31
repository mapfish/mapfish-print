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
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
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

import static org.junit.Assert.assertEquals;

/**
 * Basic test of the Map processor.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class CreateMapPagesProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "paging_processor_test/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private Map<String, OutputFormat> outputFormat;
    @Autowired
    ForkJoinPool forkJoinPool;

    @Before
    public void setUp() throws Exception {
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

    }

    @Test
    @DirtiesContext
    public void testExecute() throws Exception {
        Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
//        testPrint(config, requestData, "default-aoi", format);
//
//        getAreaOfInterest(requestData).put("display", "CLIP");
//        testPrint(config, requestData, "clip-full-aoi", format);
//        getAreaOfInterest(requestData).remove("display");
//
//        requestData = loadJsonRequestData();
//
//        getPagingAttributes(requestData).put("aoiDisplay", "clip");
//        testPrint(config, requestData, "clip-page-aoi", format);
//
//        getPagingAttributes(requestData).put("aoiDisplay", "render");
//        testPrint(config, requestData, "default-aoi", format);
//
//        getPagingAttributes(requestData).put("aoiDisplay", "none");
//        testPrint(config, requestData, "none-aoi", format);
//
//        getAreaOfInterest(requestData).put("display", "CLIP");
//        getPagingAttributes(requestData).put("aoiDisplay", "RENDER");
//        testPrint(config, requestData, "full-clip-sub-render", format);
//
//        getAreaOfInterest(requestData).put("display", "CLIP");
//        getPagingAttributes(requestData).put("aoiDisplay", "NONE");
//        testPrint(config, requestData, "full-clip-sub-none", format);
//
//        getAreaOfInterest(requestData).put("display", "CLIP");
//        getPagingAttributes(requestData).put("aoiDisplay", "NONE");
//        testPrint(config, requestData, "full-clip-sub-none", format);
//
//        getAreaOfInterest(requestData).put("display", "NONE");
//        getPagingAttributes(requestData).put("aoiDisplay", "NONE");
//        testPrint(config, requestData, "all-none", format);

        config = configurationFactory.getConfig(getFile(BASE_DIR + "config-scalebar.yaml"));
        requestData = loadJsonRequestData();
        testPrint(config, requestData, "scalebar", format);
    }

    private JSONObject getAreaOfInterest(PJsonObject requestData) throws JSONException {
        return getMapAttributes(requestData).getJSONObject("areaOfInterest");
    }

    private JSONObject getPagingAttributes(PJsonObject requestData) throws JSONException {
        final PJsonObject attributes = requestData.getJSONObject("attributes");
        JSONObject paging = attributes.getInternalObj().optJSONObject("paging");
        if (paging == null) {
            paging = new JSONObject();
            attributes.getInternalObj().put("paging", paging);
        }
        return paging;
    }

    private JSONObject getMapAttributes(PJsonObject requestData) throws JSONException {
        final PJsonObject attributes = requestData.getJSONObject("attributes");
        return attributes.getInternalObj().optJSONObject("map");
    }

    private void testPrint(Configuration config, PJsonObject requestData, String testName, AbstractJasperReportOutputFormat format) throws Exception {
        JasperPrint print = format.getJasperPrint(requestData, config, config.getDirectory(), getTaskDirectory()).print;

        assertEquals(7, print.getPages().size());
        for (int i = 0; i < print.getPages().size(); i++) {
            BufferedImage reportImage = ImageSimilarity.exportReportToImage(print, i);

//            final File output = new File("e:/tmp/test/" + testName + "/expected-page-" + i + ".png");
//            output.getParentFile().mkdirs();
//            ImageIO.write(reportImage, "png", output);

            File expectedImage = getFile(BASE_DIR + "output/"+testName+"/expected-page-" + i + ".png");
            new ImageSimilarity(reportImage, 2).assertSimilarity(expectedImage, 50);
        }
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapPagesProcessorTest.class, BASE_DIR + "requestData.json");
    }
}
