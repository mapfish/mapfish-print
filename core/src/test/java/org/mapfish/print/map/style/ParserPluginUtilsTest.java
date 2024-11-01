package org.mapfish.print.map.style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Function;
import org.geotools.api.style.Style;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

public class ParserPluginUtilsTest {

  @Test
  public void loadStyleAsURITest_ValidURI() throws IOException, URISyntaxException {
    int httpStatusCode = HttpStatus.OK.value();
    Style style = mock(Style.class);
    Optional<Style> actualStyle = testLoadingStyleWithStatusCode(style, httpStatusCode);

    assertTrue(actualStyle.isPresent());
    assertEquals(style, actualStyle.get());
  }

  private static Optional<Style> testLoadingStyleWithStatusCode(
      final Style style, final int httpStatusCode) throws IOException, URISyntaxException {
    ClientHttpRequestFactory factory = mock(ClientHttpRequestFactory.class);
    ClientHttpRequest request = mock(ClientHttpRequest.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);

    Function<byte[], Optional<Style>> function = bytes -> Optional.of(style);

    when(factory.createRequest(new URI("http://valid.uri"), HttpMethod.GET)).thenReturn(request);
    when(request.execute()).thenReturn(response);
    when(response.getRawStatusCode()).thenReturn(httpStatusCode);
    when(response.getBody())
        .thenReturn(
            new ByteArrayInputStream(
                "This is dummy style data".getBytes(Charset.defaultCharset())));

    return ParserPluginUtils.loadStyleAsURI(factory, "http://valid.uri", function);
  }

  @Test
  public void loadStyleAsURITest_InvalidStatusCode() throws IOException, URISyntaxException {
    int httpStatusCode = 999;
    Style style = mock(Style.class);
    Optional<Style> actualStyle = testLoadingStyleWithStatusCode(style, httpStatusCode);

    assertFalse(actualStyle.isPresent());
  }

  @Test
  public void loadStyleAsURITest_InValidURI() {
    ClientHttpRequestFactory factory = mock(ClientHttpRequestFactory.class);
    Style style = mock(Style.class);

    Function<byte[], Optional<Style>> function = bytes -> Optional.of(style);

    Optional<Style> actualStyle =
        ParserPluginUtils.loadStyleAsURI(factory, "invalid|uri", function);
    assertTrue(actualStyle.isEmpty());
  }
}
