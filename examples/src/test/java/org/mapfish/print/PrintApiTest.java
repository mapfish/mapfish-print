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

package org.mapfish.print;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Test the servlet print API.
 * 
 *  To run this test make sure that the test servers are running:
 * 
 *      ./gradlew examples:jettyRun
 *      
 * Or run the tests with the following task (which automatically starts the servers):
 * 
 *      ./gradlew examples:test
 *
 * @author Jesse on 5/9/2014.
 */
public class PrintApiTest extends AbstractApiTest {
    
    @Test
    public void testListApps() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.LIST_APPS_URL, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        final JSONArray appIdsJson = new JSONArray(getBodyAsText(response));
        assertTrue(appIdsJson.length() > 0);
    }
    
    @Test
    public void testGetCapabilities_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.CAPABILITIES_URL, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        assertTrue(getBodyAsText(response).contains("\"app\":\"default\""));
    }

    @Test
    public void testGetCapabilities_App() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.CAPABILITIES_URL, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        assertTrue(getBodyAsText(response).contains("\"app\":\"geoext\""));
    }

    @Test
    public void testGetCapabilities_InvalidApp() throws Exception {
        ClientHttpRequest request = getPrintRequest("INVALID-APP_ID" + MapPrinterServlet.CAPABILITIES_URL, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGetCapabilitiesPretty_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.CAPABILITIES_URL + "?pretty=true", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        assertTrue(getBodyAsText(response).contains("\n"));
    }

    @Test
    public void testGetCapabilitiesVar_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.CAPABILITIES_URL + "?var=printCapabilities", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        assertTrue(getBodyAsText(response).startsWith("printCapabilities = "));
    }

    @Test
    public void testGetCapabilitiesVarPretty_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(
                MapPrinterServlet.CAPABILITIES_URL + "?pretty=true&var=printCapabilities", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        String responseAsText = getBodyAsText(response);
        assertTrue(responseAsText.startsWith("printCapabilities = "));
        assertTrue(responseAsText.contains("\n"));
    }

    @Test
    public void testExampleRequest_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.EXAMPLE_REQUEST_URL, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        JSONObject sampleRequest = new JSONObject(getBodyAsText(response));
        assertTrue(sampleRequest.has("attributes"));
    }

    @Test
    public void testExampleRequest_App() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.EXAMPLE_REQUEST_URL, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        JSONObject sampleRequest = new JSONObject(getBodyAsText(response));
        assertTrue(sampleRequest.has("attributes"));
    }

    @Test
    public void testCreateReport_WrongMethod() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    public void testCreateReport_NoBody() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", HttpMethod.POST);
        response = request.execute();
        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testCreateReport_InvalidSpec() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", HttpMethod.POST);
        setPrintSpec("{", request);
        response = request.execute();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_App() throws Exception {
        testCreateReport("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", "examples/geoext/requestData.json");
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_NoApp() throws Exception {
        testCreateReport(MapPrinterServlet.REPORT_URL + ".pdf", "examples/verboseExample/requestData.json");
    }

    private void testCreateReport(String requestPath, String printSpec) throws Exception,
            JSONException, Exception {
        ClientHttpRequest request = getPrintRequest(requestPath, HttpMethod.POST);
        setPrintSpec(getPrintSpec(printSpec), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        
        String responseAsText = getBodyAsText(response);
        JSONObject createResult = new JSONObject(responseAsText);
        
        String ref = createResult.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        String statusUrl = createResult.getString(MapPrinterServlet.JSON_STATUS_LINK);
        String downloadUrl = createResult.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK);
        assertEquals("/print-servlet/print/status/" + ref + ".json", statusUrl);
        assertEquals("/print-servlet/print/report/" + ref, downloadUrl);
        response.close();
        
        // check status
        request = getPrintRequest(MapPrinterServlet.STATUS_URL + "/" + ref + ".json", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        
        responseAsText = getBodyAsText(response);
        JSONObject statusResult = new JSONObject(responseAsText);
        
        assertTrue(statusResult.has(MapPrinterServlet.JSON_DONE));
        assertEquals(downloadUrl, statusResult.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK));
        response.close();
        
        waitUntilDoneOrError(ref);
        
        // check download
        request = getPrintRequest(MapPrinterServlet.REPORT_URL + "/" + ref, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test(timeout = 60000)
    public void testCreateReport_InvalidFormat() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".docx", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/geoext/requestData.json"), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        
        String responseAsText = getBodyAsText(response);
        response.close();
        
        JSONObject createResult = new JSONObject(responseAsText);
        String ref = createResult.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        
        waitUntilDoneOrError(ref);
        
        // check status
        request = getPrintRequest(MapPrinterServlet.STATUS_URL + "/" + ref + ".json", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        
        responseAsText = getBodyAsText(response);
        JSONObject statusResult = new JSONObject(responseAsText);
        
        assertTrue(statusResult.has(MapPrinterServlet.JSON_ERROR));
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_App_PNG() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".png", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/geoext/requestData.json"), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        
        String responseAsText = getBodyAsText(response);
        response.close();
        
        JSONObject createResult = new JSONObject(responseAsText);
        String ref = createResult.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        
        waitUntilDoneOrError(ref);
        
        // check download
        request = getPrintRequest(MapPrinterServlet.REPORT_URL + "/" + ref, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test(timeout = 60000)
    public void testCreateAndGetReport_Success_App() throws Exception {
        ClientHttpRequest request = getPrintRequest("geoext" + MapPrinterServlet.CREATE_AND_GET_URL + ".pdf", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/geoext/requestData.json"), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test(timeout = 60000)
    public void testCreateAndGetReport_Success_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.CREATE_AND_GET_URL + ".pdf", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/verboseExample/requestData.json"), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test
    public void testGetStatus_InvalidRef() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.STATUS_URL + "/invalid-ref-number.json", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGetDownload_InvalidRef() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.REPORT_URL + "/invalid-ref-number", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private void waitUntilDoneOrError(String ref) throws Exception {
        boolean done = false;
        do {
            ClientHttpResponse response = null;
            
            try {
                ClientHttpRequest request = getPrintRequest(
                        MapPrinterServlet.STATUS_URL + "/" + ref + ".json", HttpMethod.GET);
                response = request.execute();
                final JSONObject statusResult = new JSONObject(
                        getBodyAsText(response));
                done = statusResult.getBoolean(MapPrinterServlet.JSON_DONE);
                
                if (!done) {
                    if (statusResult.has(MapPrinterServlet.JSON_ERROR)) {
                        done = true;
                    } else {
                        Thread.sleep(500);
                    }
                }
            } catch(Exception exc) {
                done = true;
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } while(!done);
    }

    protected ClientHttpRequest getPrintRequest(String path, HttpMethod method) throws IOException,
            URISyntaxException {
        return getRequest("print/" + path, method);
    }
}
