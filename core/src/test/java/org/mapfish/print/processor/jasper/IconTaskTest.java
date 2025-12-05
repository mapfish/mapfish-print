package org.mapfish.print.processor.jasper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.Processor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

public class IconTaskTest {

  @Test
  public void call_iconImageDoesNotExist_returnsMissingImageWhenOk() throws Exception {
    int statusCode = HttpStatus.OK.value();
    BufferedImage missingImage = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);

    LegendProcessor.IconTask iconTask = prepareTest(missingImage, statusCode);

    Object[] result = iconTask.call();

    assertEquals(missingImage, result[1]);
  }

  @Test
  public void call_iconImageDoesNotExist_returnsMissingImageWithUnsupportedStatus()
      throws Exception {
    int statusCode = 999;
    BufferedImage missingImage = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);

    LegendProcessor.IconTask iconTask = prepareTest(missingImage, statusCode);

    Object[] result = iconTask.call();

    assertEquals(missingImage, result[1]);
  }

  private static LegendProcessor.IconTask prepareTest(BufferedImage missingImage, int statusCode)
      throws URISyntaxException, IOException {
    LegendProcessor legendProcessorMock = spy(LegendProcessor.class);
    when(legendProcessorMock.getMissingImage()).thenReturn(missingImage);
    Processor.ExecutionContext context =
        new AbstractProcessor.Context(new HashMap<>(), new AtomicBoolean(false));
    URL icon = mock(URL.class);
    when(icon.toURI()).thenReturn(new URI("http://localhost/icon.png"));
    when(icon.getProtocol()).thenReturn("http");
    MfClientHttpRequestFactory requestFactoryMock = mock(MfClientHttpRequestFactory.class);
    MockClientHttpRequest requestMock = new MockClientHttpRequest();
    when(requestFactoryMock.createRequest(any(URI.class), any())).thenReturn(requestMock);

    byte[] responseBody = "Image bytes".getBytes(Charset.defaultCharset());
    MockClientHttpResponse responseMock = new MockClientHttpResponse(responseBody, statusCode);
    responseMock.getHeaders().setContentType(MediaType.IMAGE_PNG);
    when(requestFactoryMock.createRequest(
            new URI("testUriString"), org.springframework.http.HttpMethod.GET))
        .thenReturn(requestMock);
    requestMock.setResponse(responseMock);

    return legendProcessorMock
    .new IconTask(icon, 96, context, 1, null, requestFactoryMock, new MetricRegistry());
  }
}
