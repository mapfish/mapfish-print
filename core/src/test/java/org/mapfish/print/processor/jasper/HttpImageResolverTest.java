package org.mapfish.print.processor.jasper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

public class HttpImageResolverTest {
  @Test
  public void testResolve() throws IOException {
    int statusCode = HttpStatus.OK.value();
    HttpImageResolver httpImageResolver = new HttpImageResolver();

    BufferedImage result = resolveImageWithStatusCode(statusCode, httpImageResolver);

    // Assert that image is not the default one
    assertNotEquals(httpImageResolver.defaultImage, result);
  }

  private BufferedImage resolveImageWithStatusCode(
      int statusCode, HttpImageResolver httpImageResolver) throws IOException {
    String testUriString = "https://test.com/image.png";
    // Mock request factory and request
    MfClientHttpRequestFactory requestFactoryMock = mock(MfClientHttpRequestFactory.class);
    MockClientHttpRequest requestMock = new MockClientHttpRequest();
    when(requestFactoryMock.createRequest(any(URI.class), any())).thenReturn(requestMock);

    MockClientHttpResponse responseMock = buildMockResponse(statusCode);
    try {
      when(requestFactoryMock.createRequest(
              new URI(testUriString), org.springframework.http.HttpMethod.GET))
          .thenReturn(requestMock);
      requestMock.setResponse(responseMock);
    } catch (Exception e) {
      fail("Exception thrown: " + e.getMessage());
    }

    httpImageResolver.setUrlExtractor("(.*)");
    httpImageResolver.setUrlGroup(1);

    return httpImageResolver.resolve(requestFactoryMock, testUriString);
  }

  @Test
  public void testResolveWithUnsupportedStatus() throws IOException {
    int statusCode = 999;
    HttpImageResolver httpImageResolver = new HttpImageResolver();

    BufferedImage result = resolveImageWithStatusCode(statusCode, httpImageResolver);

    // Assert that image is the default one
    assertEquals(httpImageResolver.defaultImage, result);
  }

  private MockClientHttpResponse buildMockResponse(int statusCode) throws IOException {
    File file = AbstractMapfishSpringTest.getFile(this.getClass(), "/map-data/tiger-ny.png");
    byte[] responseBody = Files.readAllBytes(file.toPath());
    ByteArrayInputStream inputStream = new ByteArrayInputStream(responseBody);
    MockClientHttpResponse responseMock = new MockClientHttpResponse(inputStream, statusCode);
    responseMock.getHeaders().setContentType(MediaType.IMAGE_PNG);
    return responseMock;
  }

  @Test
  public void testInvalidURIResolve() {
    String invalidUriString = "htp:/invalid";
    MfClientHttpRequestFactory requestFactoryMock = mock(MfClientHttpRequestFactory.class);
    HttpImageResolver httpImageResolver = new HttpImageResolver();
    httpImageResolver.setUrlExtractor("(.*)");
    httpImageResolver.setUrlGroup(1);

    // Calls the actual method with an invalid image
    BufferedImage result = httpImageResolver.resolve(requestFactoryMock, invalidUriString);

    assertEquals(httpImageResolver.defaultImage, result);
  }

  @Test
  public void testInvalidUrlGroupResolve() throws IOException {
    String testUriString = "https://test.com/image.png";
    MfClientHttpRequestFactory requestFactoryMock = mock(MfClientHttpRequestFactory.class);
    MockClientHttpRequest requestMock = new MockClientHttpRequest();
    when(requestFactoryMock.createRequest(any(URI.class), any())).thenReturn(requestMock);

    byte[] responseBody = "Image bytes".getBytes(Charset.defaultCharset());
    MockClientHttpResponse responseMock = new MockClientHttpResponse(responseBody, HttpStatus.OK);
    responseMock.getHeaders().setContentType(MediaType.IMAGE_PNG);
    try {
      when(requestFactoryMock.createRequest(
              new URI(testUriString), org.springframework.http.HttpMethod.GET))
          .thenReturn(requestMock);
      requestMock.setResponse(responseMock);
    } catch (Exception e) {
      fail("Exception thrown: " + e.getMessage());
    }

    HttpImageResolver httpImageResolver = new HttpImageResolver();
    httpImageResolver.setUrlExtractor("(.*)");
    int urlGroup = 2;
    httpImageResolver.setUrlGroup(urlGroup); // Invalid group

    try {
      // Calls the actual method with an invalid group
      httpImageResolver.resolve(requestFactoryMock, testUriString);
      fail("Did not throw IndexOutOfBoundsException for unsupported group");
    } catch (IndexOutOfBoundsException e) {
      assertTrue(e.getMessage().contains("No group " + urlGroup));
    }
  }
}
