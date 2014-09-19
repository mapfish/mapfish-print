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

package org.mapfish.print.servlet.oldapi;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.MapPrinterServletTest;
import org.mapfish.print.servlet.ServletMapPrinterFactory;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.PObject;
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
import java.util.HashMap;
import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = {
        MapPrinterServletTest.PRINT_CONTEXT,
})
public class OldAPIMapPrinterServletTest extends AbstractMapfishSpringTest {

    @Autowired
    private OldAPIMapPrinterServlet servlet;
    @Autowired
    private MapPrinterServlet newApiServlet;
    @Autowired
    private ServletMapPrinterFactory printerFactory;

    @Test
    public void testInfoRequest() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest infoRequest = new MockHttpServletRequest();
        infoRequest.setContextPath("/print-old");
        final MockHttpServletResponse infoResponse = new MockHttpServletResponse();
        this.servlet.getInfo(null, null, infoRequest, infoResponse);
        assertEquals(HttpStatus.OK.value(), infoResponse.getStatus());
        
        final String result = infoResponse.getContentAsString();
        final PJsonObject info = parseJSONObjectFromString(result);
        
        assertTrue(info.has("scales"));
        assertTrue(info.has("dpis"));
        assertTrue(info.has("outputFormats"));
        assertTrue(info.has("layouts"));
        assertTrue(info.has("printURL"));
        assertTrue(info.has("createURL"));

        assertEquals(10, info.getArray("scales").size());
        final PObject firstScale = info.getArray("scales").getObject(0);
        assertEquals("1:5000", firstScale.getString("name"));
        assertEquals("5000", firstScale.getString("value"));

        assertEquals(5, info.getArray("dpis").size());
        
        assertTrue(info.getArray("outputFormats").size() > 0);
        assertTrue(info.getArray("outputFormats").getObject(0).has("name"));

        assertTrue(info.getArray("layouts").size() > 0);
        PObject layout = info.getArray("layouts").getObject(0);
        assertEquals("A4 Portrait", layout.getString("name"));
        assertTrue(layout.getBool("rotation"));
        assertEquals(802, layout.getObject("map").getInt("width"));
        assertEquals(210, layout.getObject("map").getInt("height"));
        
        assertEquals("/print-old/dep/print.pdf", info.getString("printURL"));
        assertEquals("/print-old/dep/create.json", info.getString("createURL"));
    }

    @Test
    public void testInfoRequestDpiSuggestions() throws Exception {
        setUpConfigFilesDpi();
        
        final MockHttpServletRequest infoRequest = new MockHttpServletRequest();
        infoRequest.setContextPath("/print-old");
        final MockHttpServletResponse infoResponse = new MockHttpServletResponse();
        this.servlet.getInfo(null, null, infoRequest, infoResponse);
        assertEquals(HttpStatus.OK.value(), infoResponse.getStatus());
        
        final String result = infoResponse.getContentAsString();
        final PJsonObject info = parseJSONObjectFromString(result);
        
        assertTrue(info.has("dpis"));
        assertEquals(4, info.getArray("dpis").size());
        final PObject firstDpi = info.getArray("dpis").getObject(0);
        assertEquals("90", firstDpi.getString("name"));
        assertEquals("90", firstDpi.getString("value"));
        final PObject lastDpi = info.getArray("dpis").getObject(3);
        assertEquals("400", lastDpi.getString("name"));
        assertEquals("400", lastDpi.getString("value"));
    }

    @Test
    public void testInfoRequestVarAndUrl() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest infoRequest = new MockHttpServletRequest();
        infoRequest.setContextPath("/print-old");
        final MockHttpServletResponse infoResponse = new MockHttpServletResponse();
        this.servlet.getInfo("http://demo.mapfish.org/2.2/print/dep/info.json", "printConfig",
                infoRequest, infoResponse);
        assertEquals(HttpStatus.OK.value(), infoResponse.getStatus());
        
        final String result = infoResponse.getContentAsString();
        assertTrue(result.startsWith("var printConfig="));
        assertTrue(result.endsWith(";"));
        
        final PJsonObject info = parseJSONObjectFromString(
                result.replace("var printConfig=", "").replace(";", ""));
        
        assertTrue(info.has("scales"));
        assertTrue(info.has("dpis"));
        assertTrue(info.has("outputFormats"));
        assertTrue(info.has("layouts"));
        assertTrue(info.has("printURL"));
        assertTrue(info.has("createURL"));
        
        assertEquals("http://demo.mapfish.org/2.2/print/dep/print.pdf", info.getString("printURL"));
        assertEquals("http://demo.mapfish.org/2.2/print/dep/create.json", info.getString("createURL"));
    }

    @Test
    public void testInfoRequestInvalidConfiguration() throws Exception {
        setUpInvalidConfigFiles();
        
        final MockHttpServletRequest infoRequest = new MockHttpServletRequest();
        infoRequest.setContextPath("/print-old");
        final MockHttpServletResponse infoResponse = new MockHttpServletResponse();
        this.servlet.getInfo(null, null, infoRequest, infoResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), infoResponse.getStatus());
        assertTrue(infoResponse.getContentAsString().contains("Error while processing request"));
    }
    
    @Test
    public void testCreateFromPostBody() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest createRequest = new MockHttpServletRequest();
        createRequest.setContextPath("/print-old");
        createRequest.setPathInfo("/create.json");
        final MockHttpServletResponse createResponse = new MockHttpServletResponse();
        
        this.servlet.createReportPost("http://demo.mapfish.org/2.2/print/dep/create.json", null,
                loadRequestDataAsString("requestData-old-api.json"), createRequest, createResponse);
        assertEquals(HttpStatus.OK.value(), createResponse.getStatus());
        
        final String result = createResponse.getContentAsString();
        final String url = parseJSONObjectFromString(result).getString("getURL");
        final String reportUrl = "http://demo.mapfish.org/2.2/print/dep/";
        assertTrue(String.format("Start of url is not as expected: \n'%s'\n'%s'", reportUrl, url), url.startsWith(reportUrl));
        assertFalse(url.startsWith(reportUrl + "report/"));
        assertTrue("Report url should end with .printout: " + url, url.endsWith(OldAPIMapPrinterServlet.REPORT_SUFFIX));
        final String printId = Files.getNameWithoutExtension(url.substring(url.lastIndexOf('/') + 1));

        final MockHttpServletResponse getReportResponse = new MockHttpServletResponse();
        newApiServlet.getReport(printId, false, getReportResponse);
        assertEquals(HttpStatus.OK.value(), getReportResponse.getStatus());

        final MockHttpServletResponse getFileResponse = new MockHttpServletResponse();
        this.servlet.getFile(printId, false, getFileResponse);
        assertEquals(HttpStatus.OK.value(), getFileResponse.getStatus());
    }

    @Test
    public void testCreateMissingSpec() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest createRequest = new MockHttpServletRequest();
        createRequest.setContextPath("/print-old");
        createRequest.setPathInfo("/create.json");
        final MockHttpServletResponse createResponse = new MockHttpServletResponse();
        
        this.servlet.createReportPost("http://demo.mapfish.org/2.2/print/pdf/create.json", null, null, createRequest, createResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), createResponse.getStatus());
        assertTrue(createResponse.getContentAsString().contains("Missing 'spec' parameter"));
    }
    
    @Test
    public void testPrint() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest createRequest = new MockHttpServletRequest();
        createRequest.setContextPath("/print-old");
        createRequest.setPathInfo("/print.pdf");
        final MockHttpServletResponse createResponse = new MockHttpServletResponse();
        
        this.servlet.printReport(loadRequestDataAsString("requestData-old-api.json"), createRequest, createResponse);
        assertEquals(HttpStatus.OK.value(), createResponse.getStatus());
    }
    
    @Test
    public void testPrintPng() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest createRequest = new MockHttpServletRequest();
        createRequest.setContextPath("/print-old");
        createRequest.setPathInfo("/print.pdf");
        final MockHttpServletResponse createResponse = new MockHttpServletResponse();
        
        this.servlet.printReport(loadRequestDataAsString("requestData-old-api-png.json"), createRequest, createResponse);
        assertEquals(HttpStatus.OK.value(), createResponse.getStatus());
        assertCorrectResponse(createResponse, "expectedSimpleImage.tiff");
    }

    private void assertCorrectResponse(MockHttpServletResponse servletGetReportResponse, String expectedImage) throws IOException {
        byte[] report = servletGetReportResponse.getContentAsByteArray();

        final String contentType = servletGetReportResponse.getHeader("Content-Type");
        assertEquals("image/png", contentType);
        String fileName = servletGetReportResponse.getHeader("Content-disposition").split("=")[1];
        assertEquals("political-boundaries.png", fileName);

        final BufferedImage reportAsImage = ImageIO.read(new ByteArrayInputStream(report));
//        ImageSimilarity.writeUncompressedImage(reportAsImage, "e:/expectedSimpleImage.tiff");
        new ImageSimilarity(reportAsImage, 2).assertSimilarity(getFile(OldAPIMapPrinterServletTest.class, "expectedSimpleImage.tiff"), 10);
    }
    
    @Test
    public void testPrintFromPostBody() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest createRequest = new MockHttpServletRequest();
        createRequest.setContextPath("/print-old");
        createRequest.setPathInfo("/print.pdf");
        final MockHttpServletResponse createResponse = new MockHttpServletResponse();
        
        this.servlet.printReportPost(loadRequestDataAsString("requestData-old-api.json"), createRequest, createResponse);
        assertEquals(HttpStatus.OK.value(), createResponse.getStatus());
    }
    
    @Test
    public void testPrintMissingSpec() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest createRequest = new MockHttpServletRequest();
        createRequest.setContextPath("/print-old");
        createRequest.setPathInfo("/print.pdf");
        final MockHttpServletResponse createResponse = new MockHttpServletResponse();
        
        this.servlet.printReportPost(null, createRequest, createResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), createResponse.getStatus());
        assertTrue(createResponse.getContentAsString().contains("Missing 'spec' parameter"));
    }
    
    @Test
    public void testGetFileNotFound() throws Exception {
        setUpConfigFiles();

        final MockHttpServletResponse getFileResponse = new MockHttpServletResponse();
        
        this.servlet.getFile("invalid-id.pdf", false, getFileResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), getFileResponse.getStatus());
    }

    @Test
    public void testInfoRequest_UrlParameterForCreateURL() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest infoRequest = new MockHttpServletRequest();
        infoRequest.setContextPath("/print-old");
        final MockHttpServletResponse infoResponse = new MockHttpServletResponse();
        this.servlet.getInfo("http://ref.geoview.bl.ch/print/wsgi/printproxy", null, infoRequest, infoResponse);
        assertEquals(HttpStatus.OK.value(), infoResponse.getStatus());

        final String result = infoResponse.getContentAsString();
        final PJsonObject info = parseJSONObjectFromString(result);

        assertEquals("http://ref.geoview.bl.ch/print/wsgi/printproxy/create.json", info.getString(OldAPIMapPrinterServlet.JSON_CREATE_URL));
        assertEquals("http://ref.geoview.bl.ch/print/wsgi/printproxy/print.pdf", info.getString(OldAPIMapPrinterServlet.JSON_PRINT_URL));
    }

    @Test
    public void testCreateFromPostBody_UrlParamForBaseURL() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest createRequest = new MockHttpServletRequest();
        createRequest.setContextPath("/print-old");
        createRequest.setPathInfo("/create.json");
        final MockHttpServletResponse createResponse = new MockHttpServletResponse();

        final String requestData = loadRequestDataAsString("requestData-old-api.json");
        this.servlet.createReportPost("http://ref.geoview.bl.ch/print/wsgi/printproxy", null,
                requestData, createRequest, createResponse);
        assertEquals(HttpStatus.OK.value(), createResponse.getStatus());

        final String result = createResponse.getContentAsString();
        final String url = parseJSONObjectFromString(result).getString("getURL");
        final String reportUrl = "http://ref.geoview.bl.ch/print/wsgi/printproxy/";
        assertTrue(String.format("Start of url is not as expected: \n'%s'\n'%s'", reportUrl, url), url.startsWith(reportUrl));
        assertTrue("Report url should end with .printout: " + url, url.endsWith(OldAPIMapPrinterServlet.REPORT_SUFFIX));
        assertFalse(url.startsWith(reportUrl + "report/"));
        final String printId = Files.getNameWithoutExtension(url.substring(url.lastIndexOf('/') + 1));

        final MockHttpServletResponse getReportResponse = new MockHttpServletResponse();
        newApiServlet.getReport(printId, false, getReportResponse);
        assertEquals(HttpStatus.OK.value(), getReportResponse.getStatus());

        final MockHttpServletResponse getFileResponse = new MockHttpServletResponse();
        this.servlet.getFile(printId, false, getFileResponse);
        assertEquals(HttpStatus.OK.value(), getFileResponse.getStatus());
    }

    private void setUpConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(OldAPIMapPrinterServletTest.class, "config-old-api.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private void setUpConfigFilesDpi() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(OldAPIMapPrinterServletTest.class, "config-old-api-dpi.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private void setUpInvalidConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private String loadRequestDataAsString(String file) throws IOException {
        final PJsonObject requestJson = parseJSONObjectFromFile(OldAPIMapPrinterServletTest.class, file);
        return requestJson.getInternalObj().toString();
    }
    
}