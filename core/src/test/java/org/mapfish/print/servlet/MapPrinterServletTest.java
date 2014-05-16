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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.json.JSONArray;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
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
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mapfish.print.servlet.ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY;

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

    @Test
    public void testExampleRequest() throws Exception {
        setUpConfigFiles();

        final MockHttpServletResponse getExampleResponseImplicit = new MockHttpServletResponse();
        this.servlet.getExampleRequest(getExampleResponseImplicit);
        assertEquals(HttpStatus.OK.value(), getExampleResponseImplicit.getStatus());
        final PJsonObject createResponseJson = parseJSONObjectFromString(getExampleResponseImplicit.getContentAsString());
        assertTrue(createResponseJson.size() > 0);

        final MockHttpServletResponse getExampleResponseExplicit = new MockHttpServletResponse();
        this.servlet.getExampleRequest(DEFAULT_CONFIGURATION_FILE_KEY, getExampleResponseExplicit);
        assertEquals(HttpStatus.OK.value(), getExampleResponseExplicit.getStatus());
        final PJsonObject createResponseJson2 = parseJSONObjectFromString(getExampleResponseExplicit.getContentAsString());
        assertTrue(createResponseJson2.size() > 0);

        final MockHttpServletResponse getExampleResponseNotFound = new MockHttpServletResponse();
        this.servlet.getExampleRequest("DoesNotExist", getExampleResponseNotFound);
        assertEquals(HttpStatus.NOT_FOUND.value(), getExampleResponseNotFound.getStatus());
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_NoAppId() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nullable
            @Override
            public MockHttpServletResponse apply(@Nullable MockHttpServletRequest servletCreateRequest) {
                try {
                    final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
                    String requestData = loadRequestDataAsString();
                    servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
                    return servletCreateResponse;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });
    }
    @Test(timeout = 60000)
    public void testCreateReport_Success_EncodedSpec() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nullable
            @Override
            public MockHttpServletResponse apply(@Nullable MockHttpServletRequest servletCreateRequest) {
                try {
                    final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
                    String requestData = URLEncoder.encode(loadRequestDataAsString(), Constants.DEFAULT_ENCODING);
                    servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
                    return servletCreateResponse;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_FormPostEncodedSpec() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nullable
            @Override
            public MockHttpServletResponse apply(@Nullable MockHttpServletRequest servletCreateRequest) {
                try {
                    final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
                    String requestData = "spec="+URLEncoder.encode(loadRequestDataAsString(), Constants.DEFAULT_ENCODING);
                    servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
                    return servletCreateResponse;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_explicitAppId() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nullable
            @Override
            public MockHttpServletResponse apply(@Nullable MockHttpServletRequest servletCreateRequest) {
                try {
                    final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
                    String requestData = loadRequestDataAsString();
                    servlet.createReport(DEFAULT_CONFIGURATION_FILE_KEY, "png", requestData, servletCreateRequest, servletCreateResponse);
                    return servletCreateResponse;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });
    }

    @Test(timeout = 60000)
    public void testCreateReport_FormPosting() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nullable
            @Override
            public MockHttpServletResponse apply(@Nullable MockHttpServletRequest servletCreateRequest) {
                try {
                    final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
                    String requestData = getFormEncodedRequestData();
                    servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
                    return servletCreateResponse;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });
    }

    private void doCreateAndPollAndGetReport(Function<MockHttpServletRequest, MockHttpServletResponse> createReport)
            throws URISyntaxException, IOException, InterruptedException, ServletException {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest("POST", "http://localhost:9834/context/print/report.png");
        servletCreateRequest.setContextPath("/print");
        final MockHttpServletResponse servletCreateResponse = createReport.apply(servletCreateRequest);

        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        String statusURL = createResponseJson.getString(MapPrinterServlet.JSON_STATUS_LINK);
        String downloadURL = createResponseJson.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK);
        assertEquals("/print/status/" + ref + ".json", statusURL);
        assertEquals("/print/report/" + ref, downloadURL);

        final String atHostRefSegment = "@" + this.servletInfo.getServletId();
        assertTrue(ref.endsWith(atHostRefSegment));
        assertTrue(ref.indexOf(atHostRefSegment) > 0);

        boolean reportReady = false;

        while (!reportReady) {
            MockHttpServletRequest servletStatusRequest = new MockHttpServletRequest("GET", statusURL);
            MockHttpServletResponse servletStatusResponse = new MockHttpServletResponse();
            servlet.getStatus(ref, servletStatusRequest, servletStatusResponse);

            final PJsonObject statusJson = parseJSONObjectFromString(servletStatusResponse.getContentAsString());
            assertTrue(statusJson.toString(), statusJson.has(MapPrinterServlet.JSON_DONE));
            assertTrue(statusJson.toString(), statusJson.has(MapPrinterServlet.JSON_TIME));
            assertTrue(statusJson.toString(), statusJson.has(MapPrinterServlet.JSON_COUNT));

            downloadURL = createResponseJson.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK);
            assertEquals("/print/report/" + ref, downloadURL);

            reportReady = statusJson.getBool(MapPrinterServlet.JSON_DONE);
            if (!reportReady) {
                Thread.sleep(500);
            }
        }

        MockHttpServletResponse servletGetReportResponse = new MockHttpServletResponse();
        servlet.getReport(ref, false, servletGetReportResponse);

        final int status = servletGetReportResponse.getStatus();
        assertEquals(HttpStatus.OK.value(), status);
        assertCorrectResponse(servletGetReportResponse);

    }

    @Test(timeout = 60000)
    public void testCreateReport_Failure() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestData.json");
        requestJson.getInternalObj().remove("attributes");
        String requestData = "{" + requestJson.toString() + "}";
        servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        final String atHostRefSegment = "@" + this.servletInfo.getServletId();
        assertTrue(ref.endsWith(atHostRefSegment));
        assertTrue(ref.indexOf(atHostRefSegment) > 0);
        waitForFailure(ref);
    }

    @Test(timeout = 60000)
    public void testCreateReport_Failure_InvalidFormat() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestData.json");
        String requestData = "{" + requestJson.toString() + "}";
        servlet.createReport("docx", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        final String atHostRefSegment = "@" + this.servletInfo.getServletId();
        assertTrue(ref.endsWith(atHostRefSegment));
        assertTrue(ref.indexOf(atHostRefSegment) > 0);
        waitForFailure(ref);
    }

    private void waitForFailure(String ref) throws IOException,
            ServletException, InterruptedException,
            UnsupportedEncodingException {
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

        this.servlet.createReportAndGetNoAppId("png", requestData, false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        assertCorrectResponse(servletCreateResponse);
    }

    private String getFormEncodedRequestData() throws IOException {
        return "spec=" + URLEncoder.encode(loadRequestDataAsString(), Constants.DEFAULT_ENCODING);
    }

    @Test(timeout = 60000)
    public void testCreateReportAndGet_Failure() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestData.json");
        requestJson.getInternalObj().remove("attributes");
        String requestData = "{" + requestJson.toString() + "}";

        this.servlet.createReportAndGetNoAppId("png", requestData, false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), servletCreateResponse.getStatus());
    }

    @Test(timeout = 60000)
    public void testCreateReportAndGet_FormPost() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = getFormEncodedRequestData();

        this.servlet.createReportAndGetNoAppId("png", requestData, false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        assertCorrectResponse(servletCreateResponse);
    }

    private void assertArrayContains(PJsonArray formats, String format) {
        for (int i = 0; i < formats.size(); i++) {
            if (format.equals(formats.getString(i))) {
                return;
            }
        }

        throw new AssertionError(format + " does is not one of the elements in: " + formats);
    }

    @Test
    public void testGetCapabilities_NotPretty() throws Exception {
        setUpConfigFiles();
        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities(false, "", servletResponse);
        assertEquals(HttpStatus.OK.value(), servletResponse.getStatus());

        final String contentAsString = servletResponse.getContentAsString();
        assertFalse(contentAsString.contains("\n"));
        final PJsonObject getInfoJson = parseJSONObjectFromString(contentAsString);

        assertTrue(getInfoJson.has("layouts"));
        final PJsonArray layouts = getInfoJson.getJSONArray("layouts");
        assertEquals(1, layouts.size());
        final PObject mainLayout = layouts.getObject(0);
        assertEquals("A4 Landscape", mainLayout.getString("name"));
        assertTrue(mainLayout.has("attributes"));
        assertEquals(2, mainLayout.getArray("attributes").size());
        assertEquals("MapAttributeValues", mainLayout.getArray("attributes").getObject(1).getString("name"));
        assertTrue(getInfoJson.has("formats"));
        final PJsonArray formats = getInfoJson.getJSONArray("formats");
        assertTrue(formats.size() > 0);
        assertArrayContains(formats, "png");
        assertArrayContains(formats, "pdf");
        assertArrayContains(formats, "xsl");

    }

    @Test
    public void testGetCapabilities_Pretty() throws Exception {
        setUpConfigFiles();
        final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities(true, "", servletResponse);
        assertEquals(HttpStatus.OK.value(), servletResponse.getStatus());

        final String contentAsString = servletResponse.getContentAsString();
        assertTrue(contentAsString.contains("\n"));
        final PJsonObject getInfoJson = parseJSONObjectFromString(contentAsString);

        assertTrue(getInfoJson.has("layouts"));
        final PJsonArray layouts = getInfoJson.getJSONArray("layouts");
        assertEquals(1, layouts.size());
        final PObject mainLayout = layouts.getObject(0);
        assertEquals("A4 Landscape", mainLayout.getString("name"));
        assertTrue(mainLayout.has("attributes"));
        assertEquals(2, mainLayout.getArray("attributes").size());
        assertEquals("MapAttributeValues", mainLayout.getArray("attributes").getObject(1).getString("name"));
        assertTrue(getInfoJson.has("formats"));
        final PJsonArray formats = getInfoJson.getJSONArray("formats");
        assertTrue(formats.size() > 0);
        assertArrayContains(formats, "png");
        assertArrayContains(formats, "pdf");
        assertArrayContains(formats, "xsl");

    }

    @Test
    public void testGetCapabilitiesWithAppId_NotPretty() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse defaultGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("default", false, "", defaultGetInfoResponse);
        assertEquals(HttpStatus.OK.value(), defaultGetInfoResponse.getStatus());

        final String contentAsString = defaultGetInfoResponse.getContentAsString();
        assertFalse(contentAsString.contains("\n"));
        final PJsonObject defaultGetInfoJson = parseJSONObjectFromString(contentAsString);
        final PJsonArray defaultLayouts = defaultGetInfoJson.getJSONArray("layouts");
        final PObject a4LandscapeLayout = defaultLayouts.getObject(0);
        assertEquals("A4 Landscape", a4LandscapeLayout.getString("name"));

        final MockHttpServletResponse app2GetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("app2", false, "", app2GetInfoResponse);
        assertEquals(HttpStatus.OK.value(), app2GetInfoResponse.getStatus());

        final PJsonObject app2GetInfoJson = parseJSONObjectFromString(app2GetInfoResponse.getContentAsString());
        final PJsonArray app2Layouts = app2GetInfoJson.getJSONArray("layouts");
        final PObject mainLayout = app2Layouts.getObject(0);
        assertEquals("main", mainLayout.getString("name"));

        final MockHttpServletResponse noSuchGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("NoSuch", false, "", noSuchGetInfoResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), noSuchGetInfoResponse.getStatus());
    }

    @Test
    public void testGetCapabilitiesWithAppId_NotPrettyAndVar() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse defaultGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("default", false, "printConfig", defaultGetInfoResponse);
        assertEquals(HttpStatus.OK.value(), defaultGetInfoResponse.getStatus());

        final String contentAsString = defaultGetInfoResponse.getContentAsString();
        assertTrue(contentAsString.startsWith("printConfig = "));
        assertTrue(contentAsString.endsWith(";"));
    }

    @Test
    public void testGetCapabilitiesWithAppId_PrettyAndVar() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse defaultGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("default", true, "printConfig", defaultGetInfoResponse);
        assertEquals(HttpStatus.OK.value(), defaultGetInfoResponse.getStatus());

        final String contentAsString = defaultGetInfoResponse.getContentAsString();
        assertTrue(contentAsString.startsWith("printConfig = "));
        assertTrue(contentAsString.endsWith(";"));
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
        return requestJson.getInternalObj().toString();
    }
}