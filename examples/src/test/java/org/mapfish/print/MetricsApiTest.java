package org.mapfish.print;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/*
 * Test the servlet metrics API.
 *
 *  Should be run inside docker composition:
 *
 *      docker compose up -d
 *      docker compose exec tests gradle :examples:test
 */
@ExtendWith(SpringExtension.class)
public class MetricsApiTest extends AbstractApiTest {

  @Test
  public void testMetrics() throws Exception {
    ClientHttpRequest request = getMetricsRequest("metrics");
    try (ClientHttpResponse response = request.execute()) {
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
      JSONObject sampleRequest = new JSONObject(getBodyAsText(response));
      assertTrue(sampleRequest.has("version"));
    }
  }

  @Test
  public void testPing() throws Exception {
    ClientHttpRequest request = getMetricsRequest("ping");
    try (ClientHttpResponse response = request.execute()) {
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals("pong", getBodyAsText(response).trim());
    }
  }

  @Test
  public void testThreads() throws Exception {
    ClientHttpRequest request = getMetricsRequest("threads");
    try (ClientHttpResponse response = request.execute()) {
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
      assertFalse(getBodyAsText(response).isEmpty());
    }
  }

  @Test
  public void testHealthcheck() throws Exception {
    ClientHttpRequest request = getMetricsRequest("healthcheck");
    try (ClientHttpResponse response = request.execute()) {
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
      String bodyAsText = getBodyAsText(response);
      assertNotNull(bodyAsText);
      JSONObject healthcheck = new JSONObject(bodyAsText);
      JSONObject jobQueueStatus = healthcheck.getJSONObject("jobQueueStatus");
      assertTrue(jobQueueStatus.getBoolean("healthy"));
      JSONObject unhealthyCountersStatus = healthcheck.getJSONObject("unhealthyCountersStatus");
      assertTrue(unhealthyCountersStatus.getBoolean("healthy"));
    }
  }

  private ClientHttpRequest getMetricsRequest(String path) throws IOException, URISyntaxException {
    return getRequest("metrics/" + path, HttpMethod.GET);
  }
}
