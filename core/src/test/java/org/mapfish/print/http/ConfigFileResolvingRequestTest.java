package org.mapfish.print.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.mapfish.print.PrintException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

public class ConfigFileResolvingRequestTest {

  @Test
  public void executeInternalShouldDoDataUriRequestWhenSchemeIsData() throws IOException {
    // Given
    var factory = mock(ConfigFileResolvingHttpRequestFactory.class);
    when(factory.getMdcContext()).thenReturn(new HashMap<>());

    // When
    var req = new ConfigFileResolvingRequest(factory, URI.create("data:/test"), HttpMethod.GET);
    var resp = req.executeInternal(new HttpHeaders());

    // Then
    assertEquals("OK", resp.getStatusText());
    resp.close();
  }

  @Test
  public void httpRequestWithRetryShouldSucceedAtSecondAttempt() throws IOException {
    // Given
    var factory = mock(ConfigFileResolvingHttpRequestFactory.class);
    var clientHttpRequest = initTestDefaultBehaviour(factory);
    var failureResponse = createResponse(500);
    var successResponse = createResponse(200);
    when(clientHttpRequest.execute()).thenReturn(failureResponse).thenReturn(successResponse);

    // When
    var req = new ConfigFileResolvingRequest(factory, URI.create("http://ex.com"), HttpMethod.GET);
    var resp = req.executeInternal(new HttpHeaders());

    // Then
    assertEquals(200, resp.getStatusCode().value());
    resp.close();
  }

  @Test
  public void httpRequestWithRetryShouldSucceedDespiteException() throws IOException {
    // Given
    var factory = mock(ConfigFileResolvingHttpRequestFactory.class);
    var clientHttpRequest = initTestDefaultBehaviour(factory);
    var failureResponse = createResponse(500);
    var successResponse = createResponse(200);
    when(clientHttpRequest.execute())
        .thenThrow(new IOException())
        .thenReturn(failureResponse)
        .thenReturn(successResponse);

    // When
    var req = new ConfigFileResolvingRequest(factory, URI.create("http://ex.com"), HttpMethod.GET);
    var resp = req.executeInternal(new HttpHeaders());

    // Then
    assertEquals(200, resp.getStatusCode().value());
    resp.close();
  }

  @Test
  public void httpRequestWithRetryDoNotBlockWhenFailing() throws IOException {
    // Given
    var factory = mock(ConfigFileResolvingHttpRequestFactory.class);
    var clientHttpRequest = initTestDefaultBehaviour(factory);
    var failureResponse = createResponse(500);
    when(clientHttpRequest.execute()).thenReturn(failureResponse);

    // When
    var req = new ConfigFileResolvingRequest(factory, URI.create("http://ex.com"), HttpMethod.GET);
    try {
      ClientHttpResponse resp = req.executeInternal(new HttpHeaders());
      fail("Should not have return the response " + resp);
      resp.close();
    } catch (PrintException e) {
      // Then
      assertEquals("Failed fetching http://ex.com", e.getMessage());
    }
  }

  private MfClientHttpRequestFactoryImpl.Request initTestDefaultBehaviour(
      ConfigFileResolvingHttpRequestFactory factory) {
    var httpRequestFactory = mock(MfClientHttpRequestFactoryImpl.class);
    var clientHttpRequest = mock(MfClientHttpRequestFactoryImpl.Request.class);

    when(httpRequestFactory.createRequest(any(URI.class), any(HttpMethod.class)))
        .thenReturn(clientHttpRequest);
    when(clientHttpRequest.getHeaders()).thenReturn(new HttpHeaders());
    when(factory.getHttpRequestFactory()).thenReturn(httpRequestFactory);
    when(factory.getMdcContext()).thenReturn(new HashMap<>());
    when(factory.getHttpRequestMaxNumberFetchRetry()).thenReturn(3);
    when(factory.getHttpRequestFetchRetryIntervalMillis()).thenReturn(50);
    return clientHttpRequest;
  }

  private ClientHttpResponse createResponse(final int statusCode) throws IOException {
    var clientHttpResponse = mock(ClientHttpResponse.class);
    when(clientHttpResponse.getStatusCode().value()).thenReturn(statusCode);
    return clientHttpResponse;
  }
}
