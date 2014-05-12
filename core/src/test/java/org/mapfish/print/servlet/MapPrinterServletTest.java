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

package org.mapfish.print.servlet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.json.JSONArray;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleBBoxGeoJsonTest;
import org.mapfish.print.util.ImageSimilarity;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
        MapPrinterServletTest.PRINT_CONTEXT,
        MapPrinterServletTest.SERVLET_CONTEXT_CONTEXT
})
public class MapPrinterServletTest extends AbstractMapfishSpringTest {

    public static final String PRINT_CONTEXT = "classpath:org/mapfish/print/servlet/mapfish-print-servlet.xml";
    public static final String SERVLET_CONTEXT_CONTEXT = "classpath:org/mapfish/print/servlet/mapfish-spring-servlet-context-config.xml";

    @Autowired
    private MapPrinterServlet servlet;
    @Autowired
    private ServletInfo servletInfo;
    @Autowired
    private ServletMapPrinterFactory printerFactory;

    @Test(timeout = 60000)
    public void testCreateReport_Success() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport(requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.PRINT_JOB_REF);

        final String atHostRefSegment = "@" + this.servletInfo.getServletId();
        assertTrue(ref.endsWith(atHostRefSegment));
        assertTrue(ref.indexOf(atHostRefSegment) > 0);
        byte[] report = null;
        while (report == null) {
            MockHttpServletResponse servletGetReportResponse = new MockHttpServletResponse();
            servlet.getReport(ref, false, servletGetReportResponse);
            final int status = servletGetReportResponse.getStatus();
            if (status == HttpStatus.ACCEPTED.value()) {
                // still processing
                Thread.sleep(500);
            } else if (status == HttpStatus.OK.value()) {
                report = assertCorrectResponse(servletGetReportResponse);
            } else {
                fail(status + " was not one of the expected response codes.  Expected either: 200 or 202");
            }
        }
    }

    @Test(timeout = 60000)
    public void testCreateReport_Failure() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = "{}";
        servlet.createReport(requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.PRINT_JOB_REF);

        final String atHostRefSegment = "@" + this.servletInfo.getServletId();
        assertTrue(ref.endsWith(atHostRefSegment));
        assertTrue(ref.indexOf(atHostRefSegment) > 0);
        while (true) {
            MockHttpServletResponse servletGetReportResponse = new MockHttpServletResponse();
            servlet.getReport(ref, false, servletGetReportResponse);
            final int status = servletGetReportResponse.getStatus();
            if (status == HttpStatus.ACCEPTED.value()) {
                // still processing
                Thread.sleep(500);
            } else if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return;
            } else {
                fail(status + " was not one of the expected response codes.  Expected either: 200 or 202");
            }
        }
    }

    @Test(timeout = 60000)
    public void testCreateReportAndGet_Success() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();

        this.servlet.createReportAndGet(requestData, false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        assertCorrectResponse(servletCreateResponse);
    }

    @Test(timeout = 60000)
    public void testCreateReportAndGet_Failure() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = "{}";

        this.servlet.createReportAndGet(requestData, false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), servletCreateResponse.getStatus());
    }

    @Test
    public void testGetInfo() throws Exception {
        setUpConfigFiles();
        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        this.servlet.getInfo(servletResponse);
        assertEquals(HttpStatus.OK.value(), servletResponse.getStatus());

        final PJsonObject getInfoJson = parseJSONObjectFromString(servletResponse.getContentAsString());

        assertTrue(getInfoJson.has("layouts"));
        final PJsonArray layouts = getInfoJson.getJSONArray("layouts");
        assertEquals(1, layouts.size());
        final PObject mainLayout = layouts.getObject(0);
        assertEquals("A4 Landscape", mainLayout.getString("name"));
        assertTrue(mainLayout.has("attributes"));
        assertEquals(3, mainLayout.getArray("attributes").size());
        assertEquals("MapAttributeValues", mainLayout.getArray("attributes").getObject(1).getString("name"));
    }

    @Test
    public void testGetInfoWithAppId() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse defaultGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getInfo("default", defaultGetInfoResponse);
        assertEquals(HttpStatus.OK.value(), defaultGetInfoResponse.getStatus());

        final PJsonObject defaultGetInfoJson = parseJSONObjectFromString(defaultGetInfoResponse.getContentAsString());
        final PJsonArray defaultLayouts = defaultGetInfoJson.getJSONArray("layouts");
        final PObject a4LandscapeLayout = defaultLayouts.getObject(0);
        assertEquals("A4 Landscape", a4LandscapeLayout.getString("name"));

        final MockHttpServletResponse app2GetInfoResponse = new MockHttpServletResponse();
        this.servlet.getInfo("app2", app2GetInfoResponse);
        assertEquals(HttpStatus.OK.value(), app2GetInfoResponse.getStatus());

        final PJsonObject app2GetInfoJson = parseJSONObjectFromString(app2GetInfoResponse.getContentAsString());
        final PJsonArray app2Layouts = app2GetInfoJson.getJSONArray("layouts");
        final PObject mainLayout = app2Layouts.getObject(0);
        assertEquals("main", mainLayout.getString("name"));

        final MockHttpServletResponse noSuchGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getInfo("NoSuch", noSuchGetInfoResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), noSuchGetInfoResponse.getStatus());
    }

    @Test
    public void testListAppIds() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);


        final MockHttpServletResponse listAppIdsResponse = new MockHttpServletResponse();
        this.servlet.listAppIds(listAppIdsResponse);
        assertEquals(HttpStatus.OK.value(), listAppIdsResponse.getStatus());

        final String contentAsString = listAppIdsResponse.getContentAsString();
        final JSONArray appIdsJson = new JSONArray(contentAsString);

        assertEquals(2, appIdsJson.length());
        Set<String> expected = Sets.newHashSet("default", "app2");
        assertTrue(expected.contains(appIdsJson.getString(0)));
        assertTrue(expected.contains(appIdsJson.getString(1)));
    }

    private byte[] assertCorrectResponse(MockHttpServletResponse servletGetReportResponse) throws IOException {
        byte[] report;
        report = servletGetReportResponse.getContentAsByteArray();

        final String contentType = servletGetReportResponse.getHeader("Content-Type");
        assertEquals("image/png", contentType);
        final Calendar instance = Calendar.getInstance();
        int year = instance.get(Calendar.YEAR);
        String fileName = servletGetReportResponse.getHeader("Content-disposition").split("=")[1];
        assertEquals("test_report-" + year + ".png", fileName);

        final BufferedImage reportAsImage = ImageIO.read(new ByteArrayInputStream(report));
        new ImageSimilarity(reportAsImage, 2).assertSimilarity(getFile(MapPrinterServletTest.class, "expectedSimpleImage.png"), 10);
        return report;
    }

    private void setUpConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private String loadRequestDataAsString() throws IOException {
        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestData.json");
        return "{" + requestJson.toString() + "}";
    }
}