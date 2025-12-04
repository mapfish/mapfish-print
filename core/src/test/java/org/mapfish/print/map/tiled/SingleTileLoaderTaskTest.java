package org.mapfish.print.map.tiled;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.mapfish.print.PrintException;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.Processor;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

public class SingleTileLoaderTaskTest {

  @Test
  public void testComputeFailOnErrorTrue() throws IOException, URISyntaxException {
    final int unsupportedStatusCode = 200;
    final String errorMessage =
        "SingleTileLoader Task stopped since fail on error parameter is enabled and the URL"
            + " http://localhost is an image format than cannot be decoded";
    ClientHttpRequest tileRequest = mock(ClientHttpRequest.class);
    CoverageTask.SingleTileLoaderTask task = prepareTest(unsupportedStatusCode, tileRequest, true);

    try {
      task.compute();
      fail("Did not throw exception indicating that the image format is not supported");
    } catch (PrintException e) {
      assertTrue(e.getCause().getMessage().contains(errorMessage));
      verify(tileRequest, times(3)).getURI();
    }
  }

  @Test
  public void testComputeFailOnErrorFalse() throws IOException, URISyntaxException {
    final int unsupportedStatusCode = 200;
    ClientHttpRequest tileRequest = mock(ClientHttpRequest.class);
    CoverageTask.SingleTileLoaderTask task = prepareTest(unsupportedStatusCode, tileRequest, false);

    CoverageTask.Tile singleTile = task.compute();
    assertNotNull(singleTile);
    verify(tileRequest, times(3)).getURI();
  }

  private CoverageTask.SingleTileLoaderTask prepareTest(
      int unsupportedStatusCode, ClientHttpRequest tileRequest, boolean failOnError)
      throws IOException, URISyntaxException {
    BufferedImage errorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Processor.ExecutionContext context =
        new AbstractProcessor.Context(new HashMap<>(), new AtomicBoolean(false));
    MetricRegistry registry = new MetricRegistry();

    // mocking ClientHttpRequest
    ClientHttpResponse clientHttpResponse = getMockResponse(unsupportedStatusCode);
    when(tileRequest.execute()).thenReturn(clientHttpResponse);
    when(tileRequest.getURI()).thenReturn(new URI("http://localhost"));

    return new CoverageTask.SingleTileLoaderTask(
        tileRequest, errorImage, 0, 0, failOnError, registry, context);
  }

  @Test
  public void testComputeWithUnsupportedStatusCode() throws IOException, URISyntaxException {
    final int unsupportedStatusCode = 999;
    ClientHttpRequest tileRequest = mock(ClientHttpRequest.class);
    CoverageTask.SingleTileLoaderTask task = prepareTest(unsupportedStatusCode, tileRequest, true);

    try {
      task.compute();
      fail("Did not throw exception with unsupported status code");
    } catch (PrintException e) {
      assertTrue(e.getCause().getMessage().contains(String.valueOf(unsupportedStatusCode)));
      verify(tileRequest, times(4)).getURI();
    }
  }

  private ClientHttpResponse getMockResponse(int rawStatusCode) throws IOException {
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(response.getStatusCode().value()).thenReturn(rawStatusCode);
    when(response.getStatusCode())
        .thenThrow(new RuntimeException("Unsupported status code has no HttpStatus"));
    when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(response.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
    return response;
  }
}
