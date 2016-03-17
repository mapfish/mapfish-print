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

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the old print API.
 * 
 *  To run this test make sure that the test servers are running:
 * 
 *      ./gradlew examples:farmRun
 *
 * Or run the tests with the following task (which automatically starts the servers):
 *
 *      ./gradlew examples:farmIntegrationTest
 */
public class OldPrintApiTest extends AbstractApiTest {
    
    @Test
    public void testInfo() throws Exception {
        ClientHttpRequest request = getPrintRequest("info.json", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
        
        JSONObject info = new JSONObject(getBodyAsText(response));
        assertTrue(info.has("scales"));
        assertTrue(info.has("dpis"));
        assertTrue(info.has("outputFormats"));
        assertTrue(info.has("layouts"));
        assertTrue(info.has("printURL"));
        assertTrue(info.has("createURL"));
    }
    
    @Test
    public void testInfoVarAndUrl() throws Exception {
        ClientHttpRequest request = getPrintRequest(
                "info.json?var=printConfig&url=http://demo.mapfish.org/2.2/print/dep/info.json", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

        final String result = getBodyAsText(response);
        assertTrue(result.startsWith("var printConfig="));
        assertTrue(result.endsWith(";"));
        
        final JSONObject info = new JSONObject(
                result.replace("var printConfig=", "").replace(";", ""));
        
        assertTrue(info.has("scales"));
        assertEquals("http://demo.mapfish.org/2.2/print/dep/print.pdf", info.getString("printURL"));
        assertEquals("http://demo.mapfish.org/2.2/print/dep/create.json", info.getString("createURL"));
    }

    @Test
    public void testInfoUrl2() throws Exception {
        ClientHttpRequest request = getPrintRequest(
                "info.json?var=printConfig&url=http%3A%2F%2Fref.geoview.bl.ch%2Fprint3%2Fwsgi%2Fprintproxy", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

        final String result = getBodyAsText(response);
        assertTrue(result.startsWith("var printConfig="));
        assertTrue(result.endsWith(";"));

        final JSONObject info = new JSONObject(
                result.replace("var printConfig=", "").replace(";", ""));

        assertTrue(info.has("scales"));
        assertEquals("http://ref.geoview.bl.ch/print3/wsgi/printproxy/print.pdf", info.getString("printURL"));
        assertEquals("http://ref.geoview.bl.ch/print3/wsgi/printproxy/create.json", info.getString("createURL"));
    }

    @Test
    public void testCreate() throws Exception {
        ClientHttpRequest request = getPrintRequest("create.json", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/verboseExample/old-api-requestData.json"), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

        final JSONObject result = new JSONObject(getBodyAsText(response));
        response.close();
        
        String getUrl = result.getString("getURL");
        final String prefix = "/print/print/dep/";
        assertTrue(String.format("Start of url is not as expected: \n'%s'\n'%s'", prefix, getUrl), getUrl.startsWith(prefix));
        assertTrue("Report url should end with .printout: " + getUrl, getUrl.endsWith(".printout"));
          
        ClientHttpRequest requestGetPdf = getRequest(getUrl.replace("/print/", ""), HttpMethod.GET);
        response = requestGetPdf.execute();
        assertEquals(response.getStatusText(), HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test
    public void testCreate_MissingSpec() throws Exception {
        ClientHttpRequest request = getPrintRequest("create.json", HttpMethod.POST);
        response = request.execute();
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    }

    @Test
    public void testCreate_InvalidSpec() throws Exception {
        ClientHttpRequest request = getPrintRequest("create.json", HttpMethod.POST);
        setPrintSpec("{}", request);
        response = request.execute();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void testCreate_Var() throws Exception {
        String url = "create.json?url=" +
                URLEncoder.encode("http://localhost:8080/print/print/dep/create.json", Constants.DEFAULT_ENCODING);
        ClientHttpRequest request = getPrintRequest(url, HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/verboseExample/old-api-requestData.json"), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

        final JSONObject result = new JSONObject(getBodyAsText(response));
        response.close();
        
        String getUrl = result.getString("getURL");
        final String prefix = "http://localhost:8080/print/print/dep/";
        assertTrue(String.format("Start of url is not as expected: \n'%s'\n'%s'", prefix, getUrl), getUrl.startsWith(prefix));
        assertTrue("Report url should end with .printout: " + getUrl, getUrl.endsWith(".printout"));

        ClientHttpRequest requestGetPdf = httpRequestFactory.createRequest(new URI(getUrl), HttpMethod.GET);
        response = requestGetPdf.execute();
        assertEquals(response.getStatusText(), HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test
    public void testPrint_SpecAsPostBody() throws Exception {
        ClientHttpRequest request = getPrintRequest("print.pdf", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/verboseExample/old-api-requestData.json"), request);
        response = request.execute();
        assertEquals(response.getStatusText(), HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test
    public void testPrint_SpecAsFormPost() throws Exception {
        ClientHttpRequest request = getPrintRequest("print.pdf", HttpMethod.POST);
        setPrintSpec("spec=" + getPrintSpec("examples/verboseExample/old-api-requestData.json"), request);
        response = request.execute();
        assertEquals(response.getStatusText(), HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test
    public void testPrint_MissingSpecPostBody() throws Exception {
        ClientHttpRequest request = getPrintRequest("print.pdf", HttpMethod.POST);
        response = request.execute();
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    }

    @Test
    public void testPrint_InvalidSpecAsPostBody() throws Exception {
        ClientHttpRequest request = getPrintRequest("print.pdf", HttpMethod.POST);
        setPrintSpec("{}", request);
        response = request.execute();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void testPrint_SpecAsGetParameter() throws Exception {
        String printSpec = getPrintSpec("examples/verboseExample/old-api-requestData.json");
        String url = "print.pdf?spec=" + URLEncoder.encode(printSpec, Constants.DEFAULT_ENCODING);
        ClientHttpRequest request = getPrintRequest(url, HttpMethod.GET);
        response = request.execute();
        assertEquals(response.getStatusText(), HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders()
                .getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    @Test
    public void testPrint_MissingSpecGet() throws Exception {
        ClientHttpRequest request = getPrintRequest("print.pdf", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void testPrint_InvalidSpecAsGetParameter() throws Exception {
        String url = "print.pdf?spec=" + URLEncoder.encode("{}", Constants.DEFAULT_ENCODING);
        ClientHttpRequest request = getPrintRequest(url, HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void testGetFile_InvalidKey() throws Exception {
        ClientHttpRequest request = getPrintRequest("invalid-key.pdf.printout", HttpMethod.GET);
        response = request.execute();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    @Test
    public void testCreate_Url2() throws Exception {
        String url = "create.json?url=" +
                     URLEncoder.encode("http://localhost:8080/print/print/dep", Constants.DEFAULT_ENCODING);
        ClientHttpRequest request = getPrintRequest(url, HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/verboseExample/old-api-requestData.json"), request);
        response = request.execute();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

        final JSONObject result = new JSONObject(getBodyAsText(response));
        response.close();

        String getUrl = result.getString("getURL");
        assertTrue(getUrl.startsWith("http://localhost:8080/print/print/dep/"));
        assertTrue("Report url should end with .printout: " + getUrl, getUrl.endsWith(".printout"));

        ClientHttpRequest requestGetPdf = httpRequestFactory.createRequest(new URI(getUrl), HttpMethod.GET);
        response = requestGetPdf.execute();
        assertEquals(response.getStatusText(), HttpStatus.OK, response.getStatusCode());
        assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
        assertTrue(response.getBody().read() >= 0);
    }

    protected ClientHttpRequest getPrintRequest(String path, HttpMethod method) throws IOException,
            URISyntaxException {
        return getRequest("print/dep/" + path, method);
    }

}
