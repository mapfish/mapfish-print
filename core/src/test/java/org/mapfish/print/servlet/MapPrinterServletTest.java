package org.mapfish.print.servlet;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.map.CreateMapProcessorFlexibleScaleBBoxGeoJsonTest;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.mapfish.print.test.util.ImageSimilarity;

import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mapfish.print.servlet.ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY;

@ContextConfiguration(locations = {
        MapPrinterServletTest.PRINT_CONTEXT
})
public class MapPrinterServletTest extends AbstractMapfishSpringTest {

    public static final String PRINT_CONTEXT = "classpath:org/mapfish/print/servlet/mapfish-print-servlet.xml";

    @Autowired
    private MapPrinterServlet servlet;
    @Autowired
    private ServletInfo servletInfo;
    @Autowired
    private ServletMapPrinterFactory printerFactory;
    @Autowired
    private ThreadPoolJobManager jobManager;
    @Autowired
    private TestHttpClientFactory requestFactory;


    @Test
    public void testExampleRequest() throws Exception {
        setUpConfigFiles();

        final MockHttpServletResponse getExampleResponseImplicit = new MockHttpServletResponse();
        this.servlet.getExampleRequest("", getExampleResponseImplicit);
        assertEquals(HttpStatus.OK.value(), getExampleResponseImplicit.getStatus());
        final PJsonObject createResponseJson = parseJSONObjectFromString(getExampleResponseImplicit.getContentAsString());
        assertTrue(createResponseJson.size() > 0);

        String example = createResponseJson.getString("requestData");
        JSONObject obj = new JSONObject(example);
        assertTrue(obj.length() > 0);

        final MockHttpServletResponse getExampleResponseExplicit = new MockHttpServletResponse();
        this.servlet.getExampleRequest(DEFAULT_CONFIGURATION_FILE_KEY, "", getExampleResponseExplicit);
        assertEquals(HttpStatus.OK.value(), getExampleResponseExplicit.getStatus());
        final PJsonObject createResponseJson2 = parseJSONObjectFromString(getExampleResponseExplicit.getContentAsString());
        assertTrue(createResponseJson2.size() > 0);

        final MockHttpServletResponse getExampleResponseNotFound = new MockHttpServletResponse();
        this.servlet.getExampleRequest("DoesNotExist", "", getExampleResponseNotFound);
        assertEquals(HttpStatus.NOT_FOUND.value(), getExampleResponseNotFound.getStatus());
    }

    @Test
    public void testExampleRequest_Jsonp() throws Exception {
        setUpConfigFiles();

        final MockHttpServletResponse getExampleResponseImplicit = new MockHttpServletResponse();
        this.servlet.getExampleRequest("exampleRequest", getExampleResponseImplicit);
        assertEquals(HttpStatus.OK.value(), getExampleResponseImplicit.getStatus());

        final String contentAsString = getExampleResponseImplicit.getContentAsString();
        assertTrue(contentAsString.startsWith("exampleRequest("));
        assertTrue(contentAsString.endsWith(");"));

        final MockHttpServletResponse getExampleResponseExplicit = new MockHttpServletResponse();
        this.servlet.getExampleRequest(DEFAULT_CONFIGURATION_FILE_KEY, "", getExampleResponseExplicit);
        assertEquals(HttpStatus.OK.value(), getExampleResponseExplicit.getStatus());

        final String contentAsString2 = getExampleResponseImplicit.getContentAsString();
        assertTrue(contentAsString2.startsWith("exampleRequest("));
        assertTrue(contentAsString2.endsWith(");"));
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_NoAppId() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nonnull
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
        }, false);
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_EncodedSpec() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nonnull
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
        }, false);
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_FormPostEncodedSpec() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nonnull
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
        }, false);
    }

    @Test(timeout = 60000)
    public void testCreateReport_Success_explicitAppId() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nonnull
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
        }, false);
    }

    @Test(timeout = 60000)
    public void testCreateReport_FormPosting() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nonnull
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
        }, true);
    }

    @Test(timeout = 60000)
    public void testCreateReport_2Requests_Success_NoAppId() throws Exception {
        doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nonnull
            @Override
            public MockHttpServletResponse apply(@Nullable MockHttpServletRequest servletCreateRequest) {
                try {
                    final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
                    String requestData = loadRequestDataAsString();
                    servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);

                    // make a 2nd request, but ignore the result
                    final MockHttpServletResponse dummyResponse = new MockHttpServletResponse();
                    servlet.createReport("png", requestData, servletCreateRequest, dummyResponse);

                    return servletCreateResponse;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        }, false);
    }

    @Test(timeout = 60000)
    @DirtiesContext
    public void testCreateReport_ForwardHttpRequests() throws Exception {
        final String geotiffURL = "http://server.com/sampleGeoTiff.tif";

        final MockClientHttpRequest request = new MockClientHttpRequest();
        ClientHttpResponse response = new MockClientHttpResponse(Files.toByteArray(getFile("sampleGeoTiff.tif")), HttpStatus.OK);
        request.setResponse(response);

        requestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input != null && input.toString().equals(geotiffURL);
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws IOException, Exception {
                return request;
            }
        });

        String ref = doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
            @Nullable
            @Override
            public MockHttpServletResponse apply(@Nullable MockHttpServletRequest servletCreateRequest) {
                try {
                    final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
                    servletCreateRequest.addHeader("Cookies", "CookieValue");
                    servletCreateRequest.addHeader("Cookies", "CookieValue2");
                    servletCreateRequest.addHeader("Header2", "h2");

                    JSONObject rawRequestData = new JSONObject(loadRequestDataAsString());
                    rawRequestData.getJSONObject("attributes").getJSONObject("imageMap").getJSONArray("layers").getJSONObject(0).
                            put("url", geotiffURL);
                    String requestData = rawRequestData.toString(2);
                    servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);

                    return servletCreateResponse;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        }, false);

        assertEquals(2, request.getHeaders().size());
        assertArrayEquals(new Object[]{"CookieValue", "CookieValue2"}, request.getHeaders().get("Cookies").toArray());
        assertArrayEquals(new Object[]{ref}, request.getHeaders().get("X-Request-ID").toArray());
    }

    @Test
    public void testListFonts() throws Exception {
        String fonts = servlet.listAvailableFonts();
        JSONArray fontsJson = new JSONArray(fonts);
        assertTrue(fonts, fontsJson.length() > 0);
    }

    private String doCreateAndPollAndGetReport(Function<MockHttpServletRequest, MockHttpServletResponse> createReport, boolean checkJsonp)
            throws URISyntaxException, IOException, InterruptedException, ServletException {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest("POST", "http://localhost:9834/context/print/report.png");
        servletCreateRequest.setContextPath("/print");
        addHeaders(servletCreateRequest);
        servletCreateRequest.addHeader("X-Request-ID", "totoIs/TheBest");
        final MockHttpServletResponse servletCreateResponse = createReport.apply(servletCreateRequest);

        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);
        String statusURL = createResponseJson.getString(MapPrinterServlet.JSON_STATUS_LINK);
        String downloadURL = createResponseJson.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK);
        assertEquals("/print/status/" + ref + ".json", statusURL);
        assertEquals("/print/report/" + ref, downloadURL);

        // Check the X-Request-ID has been included in the ref
        final String[] splittedRef = ref.split("@");
        assertEquals(3, splittedRef.length);
        assertEquals("totoIs_TheBest", splittedRef[2]);
        assertEquals(this.servletInfo.getServletId(), splittedRef[1]);

        boolean reportReady = false;
        int lastTimeElapsed = 0;
        int timeElapsed;
        while (!reportReady) {
            MockHttpServletRequest servletStatusRequest = new MockHttpServletRequest("GET", statusURL);
            addHeaders(servletStatusRequest);
            MockHttpServletResponse servletStatusResponse = new MockHttpServletResponse();
            final String jsonp = (checkJsonp) ? "getStatus" : "";
            servlet.getStatus(ref, jsonp, servletStatusRequest, servletStatusResponse);

            String contentAsString = servletStatusResponse.getContentAsString();
            if (checkJsonp) {
                assertTrue(contentAsString.startsWith("getStatus("));
                assertTrue(contentAsString.endsWith(");"));
                contentAsString = contentAsString.replace("getStatus(", "").replace(");", "");
            }

            final PJsonObject statusJson = parseJSONObjectFromString(contentAsString);
            assertTrue(statusJson.toString(), statusJson.has(MapPrinterServlet.JSON_DONE));

            downloadURL = createResponseJson.getString(MapPrinterServlet.JSON_DOWNLOAD_LINK);
            assertEquals("/print/report/" + ref, downloadURL);

            timeElapsed = statusJson.getInt(MapPrinterServlet.JSON_ELAPSED_TIME);
            assertTrue(lastTimeElapsed <= timeElapsed);
            lastTimeElapsed = timeElapsed;

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

        return ref;

    }

    private void addHeaders(MockHttpServletRequest servletStatusRequest) {
        servletStatusRequest.addHeader("Cookie", "cookie=value");
        servletStatusRequest.addHeader("Referer", "http://localhost:8080/print/");
    }

    @Test(timeout = 60000)
    public void testCreateReport_Failure() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestData.json");
        requestJson.getInternalObj().put("attributes", new JSONObject());
        String requestData = requestJson.getInternalObj().toString();
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
    public void testCreateReport_FailureInvalidUrl() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestDataError.json");
        String requestData = requestJson.getInternalObj().toString();
        servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        final String atHostRefSegment = "@" + this.servletInfo.getServletId();
        assertTrue(ref.endsWith(atHostRefSegment));
        assertTrue(ref.indexOf(atHostRefSegment) > 0);
        waitForFailure(ref);

        final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
        final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref, "", statusRequest, statusResponse);

        final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
        assertEquals("true", statusJson.getString(MapPrinterServlet.JSON_DONE));
        assertTrue(statusJson.getString(MapPrinterServlet.JSON_ERROR).startsWith(
                "java.lang.IllegalArgumentException: http://invald-url.com/sampleGeoTiff.tif not registered with"));
    }

    private void waitForFailure(String ref) throws IOException, ServletException, InterruptedException {
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
                fail(status + " was not one of the expected response codes.  Expected: 500 or 202");
            }
        }
    }

    @Test(timeout = 60000)
    public void testCancel() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        // cancel directly after starting the print
        MockHttpServletResponse servletCancelResponse = new MockHttpServletResponse();
        servlet.cancel(ref, servletCancelResponse);
        assertEquals(HttpStatus.OK.value(), servletCancelResponse.getStatus());

        final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
        final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref, "", statusRequest, statusResponse);

        final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
        assertEquals("true", statusJson.getString(MapPrinterServlet.JSON_DONE));
        assertEquals("task cancelled", statusJson.getString(MapPrinterServlet.JSON_ERROR));
        assertEquals("cancelled", statusJson.getString(MapPrinterServlet.JSON_STATUS));
    }

    @Test(timeout = 60000)
    public void testCancel_Sleep() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        // sleep a bit, so that the processors have time to start ...
        Thread.sleep(500);

        // ... then cancel
        MockHttpServletResponse servletCancelResponse = new MockHttpServletResponse();
        servlet.cancel(ref, servletCancelResponse);
        assertEquals(HttpStatus.OK.value(), servletCancelResponse.getStatus());

        final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
        final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref, "", statusRequest, statusResponse);

        final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
        assertEquals("true", statusJson.getString(MapPrinterServlet.JSON_DONE));
        assertEquals("task cancelled", statusJson.getString(MapPrinterServlet.JSON_ERROR));
        assertEquals("cancelled", statusJson.getString(MapPrinterServlet.JSON_STATUS));
    }

    @Test(timeout = 60000)
    public void testCancel_FinishedJob() throws Exception {
        // start a job and wait until it is finished
        String ref = doCreateAndPollAndGetReport(new Function<MockHttpServletRequest, MockHttpServletResponse>() {
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
        }, false);

        // ... then cancel
        MockHttpServletResponse servletCancelResponse = new MockHttpServletResponse();
        servlet.cancel(ref, servletCancelResponse);
        assertEquals(HttpStatus.OK.value(), servletCancelResponse.getStatus());

        final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
        final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref, "", statusRequest, statusResponse);

        final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
        assertEquals("true", statusJson.getString(MapPrinterServlet.JSON_DONE));
        assertEquals("task cancelled", statusJson.getString(MapPrinterServlet.JSON_ERROR));
        assertEquals("cancelled", statusJson.getString(MapPrinterServlet.JSON_STATUS));
    }

    @Test(timeout = 60000)
    public void testCancel_WrongRef() throws Exception {
        setUpConfigFiles();

        MockHttpServletResponse servletCancelResponse = new MockHttpServletResponse();
        servlet.cancel("invalid-ref", servletCancelResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), servletCancelResponse.getStatus());
    }

    @Test(timeout = 60000)
    public void testCancelSpecificAppId() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport("png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        // cancel directly after starting the print
        MockHttpServletResponse servletCancelResponse = new MockHttpServletResponse();
        servlet.cancelSpecificAppId(ref, servletCancelResponse);
        assertEquals(HttpStatus.OK.value(), servletCancelResponse.getStatus());

        final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
        final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref, "", statusRequest, statusResponse);

        final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
        assertEquals("true", statusJson.getString(MapPrinterServlet.JSON_DONE));
        assertEquals("task cancelled", statusJson.getString(MapPrinterServlet.JSON_ERROR));
        assertEquals("cancelled", statusJson.getString(MapPrinterServlet.JSON_STATUS));
    }

    @Test(timeout = 60000)
    @DirtiesContext
    public void testCreateReport_Timeout() throws Exception {
        jobManager.setTimeout(1L);
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport("timeout", "png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        final String atHostRefSegment = "@" + this.servletInfo.getServletId();
        assertTrue(ref.endsWith(atHostRefSegment));
        assertTrue(ref.indexOf(atHostRefSegment) > 0);
        waitForCanceled(ref);

        // now after canceling a task, check that the system is left in a state in which
        // it still can process jobs
        jobManager.setTimeout(600L);
        testCreateReport_Success_explicitAppId();
    }

    @Test(timeout = 60000)
    @DirtiesContext
    public void testCreateReport_AbandonedTimeout() throws Exception {
        jobManager.setAbandonedTimeout(1L);
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport("timeout", "png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        // wait for 2 seconds without making a status request, the job should be canceled after that time
        Thread.sleep(2000);

        final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
        final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref, "", statusRequest, statusResponse);

        final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
        assertEquals("true", statusJson.getString(MapPrinterServlet.JSON_DONE));
        assertEquals("task cancelled (timeout)", statusJson.getString(MapPrinterServlet.JSON_ERROR));
        assertEquals("cancelled", statusJson.getString(MapPrinterServlet.JSON_STATUS));
    }

    /**
     * Check that a job is not cancelled, when status requests are made.
     * Test case for: https://github.com/mapfish/mapfish-print/issues/404
     * @throws Exception
     */
    @Test(timeout = 60000)
    @DirtiesContext
    public void testCreateReport_NotAbandonedTimeout() throws Exception {
        jobManager.setAbandonedTimeout(1L);
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport("timeout", "png", requestData, servletCreateRequest, servletCreateResponse);
        final PJsonObject createResponseJson = parseJSONObjectFromString(servletCreateResponse.getContentAsString());
        assertTrue(createResponseJson.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        String ref = createResponseJson.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        // check that the job is not cancelled when doing status requests
        for (int i = 0; i < 4; i++) {
            Thread.sleep(500);

            final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
            final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
            servlet.getStatus(ref, "", statusRequest, statusResponse);
            final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
            assertNotEquals("cancelled", statusJson.getString(MapPrinterServlet.JSON_STATUS));
        }
        waitForNotCanceled(ref);
    }

    private void waitForCanceled(String ref) throws IOException, ServletException, InterruptedException {
        while (true) {
            MockHttpServletResponse servletGetReportResponse = new MockHttpServletResponse();
            servlet.getReport(ref, false, servletGetReportResponse);
            final int status = servletGetReportResponse.getStatus();
            if (status == HttpStatus.ACCEPTED.value()) {
                // still processing
                Thread.sleep(500);
            } else if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                String error = servletGetReportResponse.getContentAsString();
                assertTrue(error.contains("cancelled"));
                return;
            } else {
                fail(status + " was not one of the expected response codes.  Expected: 500 or 202");
            }
        }
    }

    private void waitForNotCanceled(String ref) throws IOException, ServletException, InterruptedException {
        while (true) {
            MockHttpServletResponse servletGetReportResponse = new MockHttpServletResponse();
            servlet.getReport(ref, false, servletGetReportResponse);
            final int status = servletGetReportResponse.getStatus();
            if (status == HttpStatus.ACCEPTED.value()) {
                // still processing
                Thread.sleep(500);
            } else if (status == HttpStatus.OK.value()) {
                // report was created, ok
                return;
            } else if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                fail("should not be cancelled");
            } else {
                fail(status + " was not one of the expected response codes.  Expected: 500 or 202");
            }
        }
    }

    @Test(timeout = 60000)
    public void testCreateReport_Failure_InvalidFormat() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
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

    @Test(timeout = 60000)
    public void testCreateReportAndGet_OutputName() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestData.json");
        requestJson.getInternalObj().remove("outputFilename");

        this.servlet.createReportAndGetNoAppId("png", requestJson.getInternalObj().toString(2), false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        assertCorrectResponse(servletCreateResponse, "config_test_outputname-");

        requestJson.getInternalObj().put("layout", "WithOutputName");

        this.servlet.createReportAndGetNoAppId("png", requestJson.getInternalObj().toString(2), false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.OK.value(), servletCreateResponse.getStatus());

        assertCorrectResponse(servletCreateResponse, "template_test_outputname-");
    }

    private String getFormEncodedRequestData() throws IOException {
        return "spec=" + URLEncoder.encode(loadRequestDataAsString(), Constants.DEFAULT_ENCODING);
    }

    @Test(timeout = 60000)
    @DirtiesContext
    public void testCreateReportAndGet_Timeout() throws Exception {
        jobManager.setTimeout(1L);
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();

        this.servlet.createReportAndGet("timeout", "png", requestData, false, servletCreateRequest, servletCreateResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), servletCreateResponse.getStatus());
    }

    @Test(timeout = 60000)
    public void testCreateReportAndGet_Failure() throws Exception {
        setUpConfigFiles();

        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();

        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class, "requestData.json");
        requestJson.getInternalObj().put("attributes", new JSONObject());
        String requestData = requestJson.getInternalObj().toString();

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

    @Test(timeout = 60000)
    public void testGetStatus_InvalidRef() throws Exception {
        setUpConfigFiles();

        String ref = "invalid-ref";
        MockHttpServletRequest servletStatusRequest = new MockHttpServletRequest("GET",
                "/print/status/" + ref + ".json");
        MockHttpServletResponse servletStatusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref, "", servletStatusRequest, servletStatusResponse);

        assertEquals(HttpStatus.NOT_FOUND.value(), servletStatusResponse.getStatus());
    }

    @Test(timeout = 60000)
    public void testGetReport_InvalidRef() throws Exception {
        setUpConfigFiles();

        String ref = "invalid-ref";
        MockHttpServletResponse servletGetReportResponse = new MockHttpServletResponse();
        servlet.getReport(ref, false, servletGetReportResponse);

        final int status = servletGetReportResponse.getStatus();
        assertEquals(HttpStatus.NOT_FOUND.value(), status);
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
        assertEquals(2, layouts.size());
        final PObject mainLayout = layouts.getObject(0);
        assertEquals("A4 Landscape", mainLayout.getString("name"));
        assertTrue(mainLayout.has("attributes"));
        assertEquals(2, mainLayout.getArray("attributes").size());
        assertEquals("imageMap",
                mainLayout.getArray("attributes").getObject(1).getString("name"));
        assertTrue(getInfoJson.has("formats"));
        final PJsonArray formats = getInfoJson.getJSONArray("formats");
        assertCapabilitiesFormats(formats);

    }

    private void assertCapabilitiesFormats(PJsonArray formats) {
        assertTrue(formats.size() > 0);
        assertArrayContains(formats, "png");
        assertArrayContains(formats, "pdf");
        assertArrayContains(formats, "tiff");
        assertArrayContains(formats, "tif");
        assertArrayContains(formats, "gif");
        assertArrayContains(formats, "bmp");
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
        assertEquals(2, layouts.size());
        final PObject mainLayout = layouts.getObject(0);
        assertEquals("A4 Landscape", mainLayout.getString("name"));
        assertTrue(mainLayout.has("attributes"));
        assertEquals(2, mainLayout.getArray("attributes").size());
        assertEquals("imageMap",
                mainLayout.getArray("attributes").getObject(1).getString("name"));
        assertTrue(getInfoJson.has("formats"));
        final PJsonArray formats = getInfoJson.getJSONArray("formats");
        assertCapabilitiesFormats(formats);
    }

    @Test
    public void testGetCapabilitiesWithAppId_NotPretty() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class,
                "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").
                getAbsolutePath());
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

        final PJsonObject app2GetInfoJson = parseJSONObjectFromString(
                app2GetInfoResponse.getContentAsString());
        final PJsonArray app2Layouts = app2GetInfoJson.getJSONArray("layouts");
        final PObject mainLayout = app2Layouts.getObject(0);
        assertEquals("main", mainLayout.getString("name"));

        final MockHttpServletResponse noSuchGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("NoSuch", false, "", noSuchGetInfoResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), noSuchGetInfoResponse.getStatus());
    }

    @Test
    public void testGetCapabilitiesWithAppId_NotPrettyAndJsonp() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class,
                "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").
                getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse defaultGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("default", false, "printConfig",
                defaultGetInfoResponse);
        assertEquals(HttpStatus.OK.value(), defaultGetInfoResponse.getStatus());

        final String contentAsString = defaultGetInfoResponse.getContentAsString();
        assertTrue(contentAsString.startsWith("printConfig("));
        assertTrue(contentAsString.endsWith(");"));
    }

    @Test
    public void testGetCapabilitiesWithAppId_PrettyAndJsonp() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class,
                "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").
                getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse defaultGetInfoResponse = new MockHttpServletResponse();
        this.servlet.getCapabilities("default", true, "printConfig",
                defaultGetInfoResponse);
        assertEquals(HttpStatus.OK.value(), defaultGetInfoResponse.getStatus());

        final String contentAsString = defaultGetInfoResponse.getContentAsString();
        assertTrue(contentAsString.startsWith("printConfig("));
        assertTrue(contentAsString.endsWith(");"));
    }

    @Test
    public void testListAppIds() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default",
                getFile(MapPrinterServletTest.class, "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").
                getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse listAppIdsResponse = new MockHttpServletResponse();
        this.servlet.listAppIds("", listAppIdsResponse);
        assertEquals(HttpStatus.OK.value(), listAppIdsResponse.getStatus());

        final String contentAsString = listAppIdsResponse.getContentAsString();
        final JSONArray appIdsJson = new JSONArray(contentAsString);

        assertEquals(2, appIdsJson.length());
        Set<String> expected = Sets.newHashSet("default", "app2");
        assertTrue(expected.contains(appIdsJson.getString(0)));
        assertTrue(expected.contains(appIdsJson.getString(1)));
    }

    @Test
    public void testListAppIds_Jsonp() throws Exception {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class,
                "config.yaml").getAbsolutePath());
        configFiles.put("app2", getFile(CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.class,
                CreateMapProcessorFlexibleScaleBBoxGeoJsonTest.BASE_DIR + "config.yaml").
                getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);

        final MockHttpServletResponse listAppIdsResponse = new MockHttpServletResponse();
        this.servlet.listAppIds("listAppIds", listAppIdsResponse);
        assertEquals(HttpStatus.OK.value(), listAppIdsResponse.getStatus());

        final String contentAsString = listAppIdsResponse.getContentAsString();
        assertTrue(contentAsString.startsWith("listAppIds("));
        assertTrue(contentAsString.endsWith(");"));
    }

    @Test(timeout = 60000)
    @DirtiesContext
    public void testCreateReport_ExceedsMaxNumberOfWaitingJobs() throws Exception {
        jobManager.setMaxNumberOfWaitingJobs(2);
        setUpConfigFiles();

        String requestData = loadRequestDataAsString();
        // create a few jobs to fill up the queue
        for (int i = 0; i < 20; i++) {
            servlet.createReport("timeout", "png", requestData, new MockHttpServletRequest(),
                    new MockHttpServletResponse());
        }

        // then check that jobs are rejected
        final MockHttpServletRequest servletCreateRequest = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse = new MockHttpServletResponse();
        servlet.createReport("timeout", "png", requestData, servletCreateRequest,
                servletCreateResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), servletCreateResponse.getStatus());
    }

    @Test(timeout = 60000)
    @DirtiesContext
    public void testGetStatus_WaitingRunning() throws Exception {
        jobManager.setMaxNumberOfRunningPrintJobs(1);
        setUpConfigFiles();
        jobManager.init();

        // create 2 print jobs that will run +2s. because only one job can run at a time,
        // the 2nd job will have to wait.

        // 1st job
        final MockHttpServletRequest servletCreateRequest1 = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse1 = new MockHttpServletResponse();

        String requestData = loadRequestDataAsString();
        servlet.createReport("timeout", "png", requestData, servletCreateRequest1,
                servletCreateResponse1);
        final PJsonObject createResponseJson1 =
                parseJSONObjectFromString(servletCreateResponse1.getContentAsString());
        assertTrue(createResponseJson1.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse1.getStatus());

        // 2nd job
        final MockHttpServletRequest servletCreateRequest2 = new MockHttpServletRequest();
        final MockHttpServletResponse servletCreateResponse2 = new MockHttpServletResponse();

        servlet.createReport("timeout", "png", requestData, servletCreateRequest2,
                servletCreateResponse2);
        final PJsonObject createResponseJson2 =
                parseJSONObjectFromString(servletCreateResponse2.getContentAsString());
        assertTrue(createResponseJson2.has(MapPrinterServlet.JSON_PRINT_JOB_REF));
        assertEquals(HttpStatus.OK.value(), servletCreateResponse2.getStatus());
        String ref2 = createResponseJson2.getString(MapPrinterServlet.JSON_PRINT_JOB_REF);

        final MockHttpServletRequest statusRequest = new MockHttpServletRequest();
        final MockHttpServletResponse statusResponse = new MockHttpServletResponse();
        servlet.getStatus(ref2, "", statusRequest, statusResponse);

        final PJsonObject statusJson = parseJSONObjectFromString(statusResponse.getContentAsString());
        assertEquals("false", statusJson.getString(MapPrinterServlet.JSON_DONE));
        assertEquals("waiting", statusJson.getString(MapPrinterServlet.JSON_STATUS));
    }

    private void assertCorrectResponse(MockHttpServletResponse servletGetReportResponse) throws IOException {
        assertCorrectResponse(servletGetReportResponse, "test_report-");
    }

    private byte[] assertCorrectResponse(
            MockHttpServletResponse servletGetReportResponse, String outputNamePrefix) throws IOException {
        byte[] report = servletGetReportResponse.getContentAsByteArray();

        final String contentType = servletGetReportResponse.getHeader("Content-Type");
        assertEquals("image/png", contentType);
        String fileName = servletGetReportResponse.getHeader("Content-disposition").split("=")[1];
        final Calendar instance = Calendar.getInstance();
        int year = instance.get(Calendar.YEAR);

        assertEquals(outputNamePrefix + year + ".png", fileName);

        new ImageSimilarity(getFile(MapPrinterServletTest.class,"expectedSimpleImage.png"))
                .assertSimilarity(report, 1);
        return report;
    }

    private void setUpConfigFiles() throws URISyntaxException {
        final HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("default", getFile(MapPrinterServletTest.class,
                "config.yaml").getAbsolutePath());
        configFiles.put("timeout", getFile(MapPrinterServletTest.class,
                "config-timeout.yaml").getAbsolutePath());
        printerFactory.setConfigurationFiles(configFiles);
    }

    private String loadRequestDataAsString() throws IOException {
        final PJsonObject requestJson = parseJSONObjectFromFile(MapPrinterServletTest.class,
                "requestData.json");
        return requestJson.getInternalObj().toString();
    }

    /**
     * Processor which sleeps for 2 seconds, to test timeouts.
     */
    public static class SleepProcessor extends AbstractProcessor<Void, Void> {

        public SleepProcessor() {
            super(Void.class);
        }

        @Override
        public final Void execute(final Void nothing, final ExecutionContext context) throws Exception {
            Thread.sleep(2000L);
            return null;
        }

        @Override
        public Void createInputParameter() {
            return null;
        }

        @Override
        protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
        }
    }
}
