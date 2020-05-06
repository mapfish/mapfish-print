package org.mapfish.print;

import org.apache.commons.io.IOUtils;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the servlet print API.
 * <p>
 * To run this test make sure that the test servers are running:
 * <p>
 * ./gradlew examples:farmRun
 * <p>
 * Or run the tests with the following task (which automatically starts the servers):
 * <p>
 * ./gradlew examples:geoserver
 */
public class PrintApiTest extends AbstractApiTest {

    @Test
    public void testListApps() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.LIST_APPS_URL, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
            final JSONArray appIdsJson = new JSONArray(getBodyAsText(response));
            assertTrue(appIdsJson.length() > 0);
        }
    }

    @Test
    public void testListAppsJsonp() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.LIST_APPS_URL + "?jsonp=listApps", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJavaScriptMediaType(), response.getHeaders().getContentType());

            String responseAsText = getBodyAsText(response);
            assertTrue(responseAsText.startsWith("listApps("));

            responseAsText = responseAsText.replace("listApps(", "").replace(");", "");
            final JSONArray appIdsJson = new JSONArray(responseAsText);
            assertTrue(appIdsJson.length() > 0);
        }
    }

    @Test
    public void testGetCapabilities_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.CAPABILITIES_URL, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
            assertTrue(getBodyAsText(response).contains("\"app\":\"default\""));
        }
    }

    @Test
    public void testGetCapabilities_App() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.CAPABILITIES_URL, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
            assertTrue(getBodyAsText(response).contains("\"app\":\"geoext\""));
        }
    }

    @Test
    public void testGetCapabilities_InvalidApp() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("INVALID-APP_ID" + MapPrinterServlet.CAPABILITIES_URL, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    public void testGetCapabilitiesPretty_NoApp() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.CAPABILITIES_URL + "?pretty=true", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
            assertTrue(getBodyAsText(response).contains("\n"));
        }
    }

    @Test
    public void testGetCapabilitiesJsonp_NoApp() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.CAPABILITIES_URL + "?jsonp=printCapabilities",
                                HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJavaScriptMediaType(), response.getHeaders().getContentType());
            assertTrue(getBodyAsText(response).startsWith("printCapabilities("));
        }
    }

    @Test
    public void testGetCapabilitiesJsonpPretty_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(
                MapPrinterServlet.CAPABILITIES_URL + "?pretty=true&jsonp=printCapabilities", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJavaScriptMediaType(), response.getHeaders().getContentType());
            String responseAsText = getBodyAsText(response);
            assertTrue(responseAsText.startsWith("printCapabilities("));
            assertTrue(responseAsText.contains("\n"));
        }
    }

    @Test
    public void testExampleRequest_NoApp() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.EXAMPLE_REQUEST_URL, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
            checkExampleRequest(getBodyAsText(response));
        }
    }

    @Test
    public void testExampleRequestJsonp_NoApp() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.EXAMPLE_REQUEST_URL + "?jsonp=exampleRequest",
                                HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJavaScriptMediaType(), response.getHeaders().getContentType());

            String responseAsText = getBodyAsText(response);
            assertTrue(responseAsText.startsWith("exampleRequest("));

            responseAsText = responseAsText.replace("exampleRequest(", "").replace(");", "");
            checkExampleRequest(responseAsText);
        }
    }

    @Test
    public void testExampleRequest_App() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.EXAMPLE_REQUEST_URL, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
            checkExampleRequest(getBodyAsText(response));
        }
    }

    @Test
    public void testExampleRequestJsonp_App() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.EXAMPLE_REQUEST_URL + "?jsonp=exampleRequest",
                                HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJavaScriptMediaType(), response.getHeaders().getContentType());

            String responseAsText = getBodyAsText(response);
            assertTrue(responseAsText.startsWith("exampleRequest("));

            responseAsText = responseAsText.replace("exampleRequest(", "").replace(");", "");
            checkExampleRequest(responseAsText);
        }
    }

    private void checkExampleRequest(String responseAsText) throws JSONException {
        JSONObject samplesRequest = new JSONObject(responseAsText);
        final Iterator keys = samplesRequest.keys();
        assertTrue(keys.hasNext());
        String key = (String) keys.next();
        JSONObject sampleRequest = new JSONObject(samplesRequest.getString(key));
        assertTrue(sampleRequest.has("attributes"));
    }

    @Test
    public void testCreateReport_WrongMethod() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        }
    }

    @Test
    public void testCreateReport_NoBody() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", HttpMethod.POST);
        try (ClientHttpResponse response = request.execute()) {
            assertNotEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    public void testCreateReport_InvalidSpec() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", HttpMethod.POST);
        setPrintSpec("{", request);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Test
    public void testCreateReport_RequestTooLarge() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", HttpMethod.POST);
        final String printSpec = getPrintSpec("examples/geoext/requestData.json");

        // create a large, fake request
        StringBuilder largeRequest = new StringBuilder();
        for (int i = 0; i < 9999; i++) {
            largeRequest.append(printSpec);
        }

        setPrintSpec(largeRequest.toString(), request);
        try (ClientHttpResponse response = request.execute()) {
            assertNotEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_App() throws Exception {
        final String printSpec = getPrintSpec("examples/geoext/requestData.json");
        testCreateReport("geoext" + MapPrinterServlet.REPORT_URL + ".pdf", printSpec);
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_NoApp() throws Exception {
        final String printSpec = getDefaultAppDefaultRequestSample();
        testCreateReport(MapPrinterServlet.REPORT_URL + ".pdf", printSpec);
    }

    private void testCreateReport(String requestPath, String printSpec) throws Exception {
        ClientHttpRequest request = getPrintRequest(requestPath, HttpMethod.POST);
        setPrintSpec(printSpec, request);
        final String ref;
        final String downloadUrl;
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

            String responseAsText = getBodyAsText(response);
            JSONObject createResult = new JSONObject(responseAsText);

            ref = createResult.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
            String statusUrl = createResult.getString(MapPrinterServlet.JSON_STATUS_LINK);
            downloadUrl = createResult.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK);
            assertEquals("/print/print/status/" + ref + ".json", statusUrl);
            assertEquals("/print/print/report/" + ref, downloadUrl);
        }

        // check status
        request = getPrintRequest(MapPrinterServlet.STATUS_URL + "/" + ref + ".json", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
            assertEquals("max-age=0, must-revalidate, no-cache, no-store",
                         String.join(", ", response.getHeaders().get("Cache-Control")));

            String responseAsText = getBodyAsText(response);
            JSONObject statusResult = new JSONObject(responseAsText);

            assertTrue(statusResult.has(MapPrinterServlet.JSON_DONE));
            assertEquals(downloadUrl, statusResult.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK));
        }

        final boolean hasAppId = !requestPath.startsWith(MapPrinterServlet.REPORT_URL);
        String appId = null;
        if (hasAppId) {
            appId = requestPath.substring(0, requestPath.indexOf('/'));

            // app specific status option
            request = getPrintRequest(appId + MapPrinterServlet.STATUS_URL + "/" + ref + ".json",
                                      HttpMethod.GET);
            try (ClientHttpResponse response = request.execute()) {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

                String responseAsText = getBodyAsText(response);
                JSONObject statusResult = new JSONObject(responseAsText);

                assertTrue(statusResult.has(MapPrinterServlet.JSON_DONE));
                assertEquals(downloadUrl, statusResult.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK));
            }
        }

        // check status with JSONP
        request = getPrintRequest(MapPrinterServlet.STATUS_URL + "/" + ref + ".json?jsonp=getStatus",
                                  HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJavaScriptMediaType(), response.getHeaders().getContentType());

            String responseAsText = getBodyAsText(response);
            assertTrue(responseAsText.startsWith("getStatus("));

            responseAsText = responseAsText.replace("getStatus(", "").replace(");", "");
            JSONObject statusResult = new JSONObject(responseAsText);

            assertTrue(statusResult.has(MapPrinterServlet.JSON_DONE));
            assertEquals(downloadUrl, statusResult.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK));
        }

        waitUntilDoneOrError(ref);

        // check download
        request = getPrintRequest(MapPrinterServlet.REPORT_URL + "/" + ref, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
            assertTrue(response.getBody().read() >= 0);
        }

        if (hasAppId) {
            // check download with appId url
            request = getPrintRequest("/" + appId + MapPrinterServlet.REPORT_URL + "/" + ref, HttpMethod.GET);
            try (ClientHttpResponse response = request.execute()) {
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
                assertTrue(response.getBody().read() >= 0);
            }
        }
    }

    @Test(timeout = 60000)
    public void testCreateReport_InvalidFormat() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".docx", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/geoext/requestData.json"), request);
        final String ref;
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

            final String responseAsText = getBodyAsText(response);
            final JSONObject createResult = new JSONObject(responseAsText);
            ref = createResult.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        }


        waitUntilDoneOrError(ref);

        // check status
        request = getPrintRequest(MapPrinterServlet.STATUS_URL + "/" + ref + ".json", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

            final String responseAsText = getBodyAsText(response);
            final JSONObject statusResult = new JSONObject(responseAsText);

            assertTrue(statusResult.has(MapPrinterServlet.JSON_ERROR));
        }
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_App_PNG() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".png", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/geoext/requestData.json"), request);
        final String ref;
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

            final String responseAsText = getBodyAsText(response);
            final JSONObject createResult = new JSONObject(responseAsText);
            ref = createResult.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        }


        waitUntilDoneOrError(ref);

        // check download
        request = getPrintRequest(MapPrinterServlet.REPORT_URL + "/" + ref, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
            assertTrue(response.getBody().read() >= 0);
        }
    }

    @Test(timeout = 60000)
    public void testCreateAndGetReport_Success_App() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.CREATE_AND_GET_URL + ".pdf", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/geoext/requestData.json"), request);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
            assertTrue(response.getBody().read() >= 0);
        }
    }

    @Test//(timeout = 60000)
    public void testCreateAndGetReport_Success_NoApp() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.CREATE_AND_GET_URL + ".pdf", HttpMethod.POST);
        String example = getDefaultAppDefaultRequestSample();
        setPrintSpec(example, request);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(new MediaType("application", "pdf"), response.getHeaders().getContentType());
            assertTrue(response.getBody().read() >= 0);
        }
    }

    private String getDefaultAppDefaultRequestSample() throws IOException, URISyntaxException, JSONException {
        ClientHttpResponse exampleResp =
                getPrintRequest(MapPrinterServlet.EXAMPLE_REQUEST_URL, HttpMethod.GET).execute();
        JSONObject examples =
                new JSONObject(IOUtils.toString(exampleResp.getBody(), "UTF-8"));
        return examples.getString(examples.keys().next());
    }

    @Test
    public void testGetStatus_InvalidRef() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.STATUS_URL + "/invalid-ref-number.json", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    public void testGetDownload_InvalidRef() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.REPORT_URL + "/invalid-ref-number", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    public void testCancel_InvalidRef() throws Exception {
        ClientHttpRequest request =
                getPrintRequest(MapPrinterServlet.CANCEL_URL + "/invalid-ref-number", HttpMethod.DELETE);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    public void testCancel() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("geoext" + MapPrinterServlet.REPORT_URL + ".png", HttpMethod.POST);
        setPrintSpec(getPrintSpec("examples/geoext/requestData.json"), request);
        final String ref;
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(getJsonMediaType(), response.getHeaders().getContentType());

            final String responseAsText = getBodyAsText(response);
            final JSONObject createResult = new JSONObject(responseAsText);
            ref = createResult.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        }


        request = getPrintRequest(MapPrinterServlet.CANCEL_URL + "/" + ref, HttpMethod.DELETE);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        request = getPrintRequest(MapPrinterServlet.STATUS_URL + "/" + ref + ".json", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            final JSONObject statusResult = new JSONObject(
                    getBodyAsText(response));
            assertTrue(statusResult.getBoolean(MapPrinterServlet.JSON_DONE));
            assertEquals("task cancelled", statusResult.getString(MapPrinterServlet.JSON_ERROR));
        }
    }

    @Test
    public void testSecuredTemplate_Capabilities() throws Exception {
        final JSONArray layouts = execCapabilitiesRequestWithAut(1, null);
        assertEquals("A4 landscape", layouts.getJSONObject(0).getString("name"));

        execCapabilitiesRequestWithAut(2, "jimi:jimi"); // jimi is admin
        execCapabilitiesRequestWithAut(1, "bob:bob"); // bob is has ROLE_USER
    }

    @Test
    public void testSecuredTemplate_Capabilities_SecUrl() throws Exception {
        assertRequiresAuth("sec/print/secured_templates" + MapPrinterServlet.CAPABILITIES_URL);
        assertRequiresAuth("sec/print" + MapPrinterServlet.CAPABILITIES_URL);
        assertRequiresAuth("sec/print/dep/info.json");
    }

    private void assertRequiresAuth(String path) throws IOException, URISyntaxException {
        ClientHttpRequest request = getRequest(path, HttpMethod.GET);
        HttpURLConnection urlConnection = (HttpURLConnection) request.getURI().toURL().openConnection();
        assertEquals(HttpStatus.FOUND.value(), urlConnection.getResponseCode());
        assertEquals("https://localhost:8443/print/" + path, urlConnection.getHeaderField("Location"));
        urlConnection.disconnect();
    }

    @Test
    public void testSecuredTemplate_CreateMap() throws Exception {
        ClientHttpRequest request =
                getPrintRequest("secured_templates" + MapPrinterServlet.CREATE_AND_GET_URL + ".pdf",
                                HttpMethod.POST);
        final String printSpec = getPrintSpec("examples/secured_templates/requestData.json")
                .replace("\"A4 landscape\"", "\"secured\"");
        setPrintSpec(printSpec, request);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        execCreateRequestWithAuth("jimi:jimi", HttpStatus.OK, printSpec);
        execCreateRequestWithAuth("bob:bob", HttpStatus.FORBIDDEN, printSpec);

    }

    @Test
    public void testCors() throws Exception {
        ClientHttpRequest request = getPrintRequest(MapPrinterServlet.LIST_APPS_URL, HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertFalse(response.getHeaders().containsKey("Access-Control-Allow-Origin"));
        }

        request = getPrintRequest(MapPrinterServlet.LIST_APPS_URL, HttpMethod.GET);
        request.getHeaders().set("Origin", "http://example.com/");
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("*", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
        }

        request = getPrintRequest(MapPrinterServlet.LIST_APPS_URL, HttpMethod.OPTIONS);
        request.getHeaders().set("Origin", "http://example.com/");
        request.getHeaders().set("Access-Control-Request-Method", "POST");
        request.getHeaders().set("Access-Control-Request-Headers", "X-Toto");
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("*", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
            assertTrue(response.getHeaders().containsKey("Access-Control-Max-Age"));
            assertTrue(response.getHeaders().containsKey("Access-Control-Allow-Methods"));
            assertEquals("X-Toto", response.getHeaders().getFirst("Access-Control-Allow-Headers"));
        }
    }

    private JSONArray execCapabilitiesRequestWithAut(int expectedNumberOfLayouts, String credentials)
            throws IOException, URISyntaxException, JSONException {
        ClientHttpRequest request =
                getPrintRequest("secured_templates" + MapPrinterServlet.CAPABILITIES_URL, HttpMethod.GET);
        if (credentials != null) {
            addAuthHeader(request, credentials);
        }
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());

            String responseAsText = getBodyAsText(response);
            JSONObject infoResult = new JSONObject(responseAsText);
            final JSONArray layouts = infoResult.getJSONArray("layouts");
            assertEquals(expectedNumberOfLayouts, layouts.length());
            return layouts;
        }
    }

    private void execCreateRequestWithAuth(String credentials, HttpStatus expectedStatus, String printSpec)
            throws IOException, URISyntaxException {
        ClientHttpRequest request =
                getPrintRequest("secured_templates" + MapPrinterServlet.CREATE_AND_GET_URL + ".pdf",
                                HttpMethod.POST);
        setPrintSpec(printSpec, request);
        addAuthHeader(request, credentials);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(response.getStatusText(), expectedStatus, response.getStatusCode());
        }
    }

    private void addAuthHeader(ClientHttpRequest request, String credentials)
            throws UnsupportedEncodingException {
        request.getHeaders().add("Authorization",
                                 "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(
                                         StandardCharsets.UTF_8)));
    }

    private void waitUntilDoneOrError(String ref) {
        boolean done = false;
        do {
            try {
                ClientHttpRequest request = getPrintRequest(
                        MapPrinterServlet.STATUS_URL + "/" + ref + ".json", HttpMethod.GET);
                try (ClientHttpResponse response = request.execute()) {
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
                }
            } catch (Exception exc) {
                done = true;
            }
        } while (!done);
    }

    protected ClientHttpRequest getPrintRequest(String path, HttpMethod method) throws IOException,
            URISyntaxException {
        return getRequest("print/" + path, method);
    }
}
