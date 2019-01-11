package org.mapfish.print;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/*
 * Test the servlet metrics API.
 *
 *  To run this test make sure that the test servers are running:
 *
 *      ./gradlew examples:jettyRun
 *
 * Or run the tests with the following task (which automatically starts the servers):
 *
 *      ./gradlew examples:test
 */
public class MetricsApiTest extends AbstractApiTest {

    @Test
    public void testMetrics() throws Exception {
        ClientHttpRequest request = getMetricsRequest("metrics", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
            JSONObject sampleRequest = new JSONObject(getBodyAsText(response));
            assertTrue(sampleRequest.has("version"));
        }
    }

    @Test
    public void testPing() throws Exception {
        ClientHttpRequest request = getMetricsRequest("ping", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("pong", getBodyAsText(response).trim());
        }
    }

    @Test
    public void testThreads() throws Exception {
        ClientHttpRequest request = getMetricsRequest("threads", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
            assertFalse(getBodyAsText(response).isEmpty());
        }
    }

    @Test
    public void testHealthcheck() throws Exception {
        ClientHttpRequest request = getMetricsRequest("healthcheck", HttpMethod.GET);
        try (ClientHttpResponse response = request.execute()) {
            // TODO not implemented?
            assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
//        assertNotNull(new JSONObject(getBodyAsText(response)));
        }
    }

    private ClientHttpRequest getMetricsRequest(String path, HttpMethod method) throws IOException,
            URISyntaxException {
        return getRequest("metrics/" + path, method);
    }

}
