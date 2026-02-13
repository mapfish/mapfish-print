package org.mapfish.print;

import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test the servlet print API.
 *
 * <p>To run this test make sure that the test servers are running:
 *
 * <p>./gradlew examples:farmRun
 *
 * <p>Or run the tests with the following task (which automatically starts the servers):
 *
 * <p>./gradlew examples:geoserver
 */
@ExtendWith(SpringExtension.class)
public class ContextPathTest extends AbstractApiTest {

  @ParameterizedTest
  @EnabledIfEnvironmentVariable(named = "MAPFISH_PRINT_TESTS_CONTEXT_PATH_MODE", matches = "true")
  @ValueSource(strings = {"print", "print/"})
  public void testIndexPaths(String path) throws Exception {
    ClientHttpRequest request = getRequest(path, HttpMethod.GET);
    try (ClientHttpResponse response = request.execute()) {
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals("text/html", response.getHeaders().getContentType().toString());
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "MAPFISH_PRINT_TESTS_CONTEXT_PATH_MODE", matches = "true")
  public void testListApps() throws Exception {
    ClientHttpRequest request =
        getRequest("print/print" + MapPrinterServlet.LIST_APPS_URL, HttpMethod.GET);
    try (ClientHttpResponse response = request.execute()) {
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(getJsonMediaType(), response.getHeaders().getContentType());
      final JSONArray appIdsJson = new JSONArray(getBodyAsText(response));
      assertFalse(appIdsJson.isEmpty());
    }
  }
}
