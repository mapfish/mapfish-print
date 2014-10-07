/*
 *
 *  * Copyright (C) 2014  Camptocamp
 *  *
 *  * This file is part of MapFish Print
 *  *
 *  * MapFish Print is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MapFish Print is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.mapfish.print.servlet;

import com.google.common.collect.Maps;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.access.AccessAssertionTestUtil;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
        MapPrinterServletSecurityTest.PRINT_CONTEXT
})
public class MapPrinterServletSecurityTest extends AbstractMapfishSpringTest {

    public static final String PRINT_CONTEXT = "classpath:org/mapfish/print/servlet/mapfish-print-servlet.xml";

    @Autowired
    private MapPrinterServlet servlet;
    @Autowired
    private ServletMapPrinterFactory printerFactory;

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test(timeout = 60000)
    public void testCreateReportAndGet_Success() throws Exception {
        AccessAssertionTestUtil.setCreds("ROLE_USER", "ROLE_EDITOR");
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();

        this.servlet.createReportAndGetNoAppId("png", requestData, false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        assertCorrectResponse(servletCreateResponse);
    }

    @Test(timeout = 60000, expected = AccessDeniedException.class)
    public void testCreateReportAndGet_InsufficientPrivileges() throws Exception {
        AccessAssertionTestUtil.setCreds("ROLE_USER");
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();

        this.servlet.createReportAndGetNoAppId("png", requestData, false, servletCreateRequest, servletCreateResponse);

    }

    @Test(timeout = 60000, expected = AuthenticationCredentialsNotFoundException.class)
    public void testCreateReportAndGet_NoCredentials() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();

        this.servlet.createReportAndGetNoAppId("png", requestData, false, servletCreateRequest, servletCreateResponse);
    }

    @Test(timeout = 60000)
    public void testCreateReportAndGet_RequestAllowed_OtherGetDenied() throws Exception {
        AccessAssertionTestUtil.setCreds("ROLE_USER", "ROLE_EDITOR");
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();

        this.servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());
        final JSONObject response = new JSONObject(servletCreateResponse.getContentAsString());

        final String ref = response.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        try {
            AccessAssertionTestUtil.setCreds("ROLE_USER");
            final MockHttpServletResponse getResponse1 = new MockHttpServletResponse();
            this.servlet.getReport(ref, false, getResponse1);
            fail("Expected an AccessDeniedException");
        } catch (AccessDeniedException e) {
            // good
        }
        SecurityContextHolder.clearContext();

        try {
            final MockHttpServletResponse getResponse2 = new MockHttpServletResponse();
            this.servlet.getReport(ref, false, getResponse2);
            assertEquals(HttpStatus.UNAUTHORIZED.value(), servletCreateResponse.getStatus());
            fail("Expected an AuthenticationCredentialsNotFoundException");
        } catch (AuthenticationCredentialsNotFoundException e) {
            // good
        }
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

        new ImageSimilarity(reportAsImage, 2).assertSimilarity(getFile(MapPrinterServletSecurityTest.class, "expectedSimpleImage.png"), 10);
        return report;
    }

    private void setUpConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletSecurityTest.class, "config-security.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private String loadRequestDataAsString() throws IOException {
        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletSecurityTest.class, "requestData.json");
        return requestJson.getInternalObj().toString();
    }

}