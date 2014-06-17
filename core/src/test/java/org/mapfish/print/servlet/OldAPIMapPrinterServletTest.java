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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.servlet.job.ThreadPoolJobManager;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.Maps;

@ContextConfiguration(locations = {
        OldAPIMapPrinterServletTest.PRINT_CONTEXT,
        OldAPIMapPrinterServletTest.SERVLET_CONTEXT_CONTEXT
})
public class OldAPIMapPrinterServletTest extends AbstractMapfishSpringTest {

    public static final String PRINT_CONTEXT = "classpath:org/mapfish/print/servlet/mapfish-print-servlet.xml";
    public static final String SERVLET_CONTEXT_CONTEXT = "classpath:org/mapfish/print/servlet/mapfish-spring-servlet-context-config.xml";

    @Autowired
    private OldAPIMapPrinterServlet servlet;
    @Autowired
    private ServletInfo servletInfo;
    @Autowired
    private ServletMapPrinterFactory printerFactory;
    @Autowired
    private ThreadPoolJobManager jobManager;

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
        
        assertTrue(info.getArray("outputFormats").size() > 0);
        assertTrue(info.getArray("outputFormats").getObject(0).has("name"));

        assertTrue(info.getArray("layouts").size() > 0);
        PObject layout = info.getArray("layouts").getObject(0);
        assertEquals("A4 Landscape", layout.getString("name"));
        assertTrue(layout.getBool("rotation"));
        assertEquals(802, layout.getObject("map").getInt("width"));
        assertEquals(500, layout.getObject("map").getInt("height"));
        
        assertEquals("/print-old/print.pdf", info.getString("printURL"));
        assertEquals("/print-old/create.json", info.getString("createURL"));
    }

    @Test
    public void testInfoRequestVarAndUrl() throws Exception {
        setUpConfigFiles();
        
        final MockHttpServletRequest infoRequest = new MockHttpServletRequest();
        infoRequest.setContextPath("/print-old");
        final MockHttpServletResponse infoResponse = new MockHttpServletResponse();
        this.servlet.getInfo("http://demo.mapfish.org/2.2/print/pdf/info.json", "printConfig",
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
        
        assertEquals("http://demo.mapfish.org/2.2/print/pdf/print.pdf", info.getString("printURL"));
        assertEquals("http://demo.mapfish.org/2.2/print/pdf/create.json", info.getString("createURL"));
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

    private void setUpConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(OldAPIMapPrinterServletTest.class, "config-old-api.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private void setUpInvalidConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(OldAPIMapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private String loadRequestDataAsString() throws IOException {
        final PJsonObject requestJson = parseJSONObjectFromFile(OldAPIMapPrinterServletTest.class, "requestData.json");
        return requestJson.getInternalObj().toString();
    }
    
}