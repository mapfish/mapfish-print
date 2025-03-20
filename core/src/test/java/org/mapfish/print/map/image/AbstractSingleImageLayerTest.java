package org.mapfish.print.map.image;

import static org.junit.Assert.assertTrue;

import com.codahale.metrics.MetricRegistry;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

public class AbstractSingleImageLayerTest {

  @Test
  public void testFetchImage() throws IOException {
    MapfishMapContext mapContext = new MapfishMapContext(null, null, 100, 100, false, true);
    MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest();
    final int unsupportedStatusCode = 999;
    mockClientHttpRequest.setResponse(
        new MockClientHttpResponse(
            "SomeResponse".getBytes(Constants.DEFAULT_CHARSET), unsupportedStatusCode));
    AbstractLayerParams layerParams = new AbstractLayerParams();
    layerParams.failOnError = true;
    AbstractSingleImageLayer layer = new AbstractSingleImageLayerTestImpl(layerParams);

    try {
      layer.fetchImage(mockClientHttpRequest, mapContext);
      Assert.fail("Did not throw exception with unsupported status code");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains(String.valueOf(unsupportedStatusCode)));
    }
  }

  private static class AbstractSingleImageLayerTestImpl extends AbstractSingleImageLayer {

    public AbstractSingleImageLayerTestImpl(AbstractLayerParams layerParams) {
      super(null, null, layerParams, new MetricRegistry(), null);
    }

    @Override
    protected BufferedImage loadImage(
        MfClientHttpRequestFactory requestFactory, MapfishMapContext transformer) {
      return null;
    }

    @Override
    public LayerContext prepareRender(
        MapfishMapContext transformer, MfClientHttpRequestFactory clientHttpRequestFactory) {
      return new LayerContext(DEFAULT_SCALING, null, null);
    }

    @Override
    public RenderType getRenderType() {
      return null;
    }
  }
}
