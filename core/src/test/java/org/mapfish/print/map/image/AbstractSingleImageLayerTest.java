package org.mapfish.print.map.image;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codahale.metrics.MetricRegistry;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
      Assertions.fail("Did not throw exception with unsupported status code");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains(String.valueOf(unsupportedStatusCode)));
    }
  }

  @Test
  public void testFetch204NonContent() throws IOException {
    MapfishMapContext mapContext =
        new MapfishMapContext(null, new Dimension(10, 10), 100, 100, false, true);
    MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest();
    final int noContentCode = 204;
    mockClientHttpRequest.setResponse(new MockClientHttpResponse(new byte[0], noContentCode));
    AbstractLayerParams layerParams = new AbstractLayerParams();
    layerParams.failOnError = true;
    AbstractSingleImageLayer layer = new AbstractSingleImageLayerTestImpl(layerParams);
    try {
      BufferedImage bufferedImage = layer.fetchImage(mockClientHttpRequest, mapContext);

      Assert.assertEquals("Image width is not correct", 10, bufferedImage.getWidth());
      Assert.assertEquals("Image height is not correct", 10, bufferedImage.getHeight());

      // check alpha chanel is equal to 0 for every pixels
      for (int x = 0; x < bufferedImage.getWidth(); x++) {
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
          int argb = bufferedImage.getRGB(x, y);
          Assert.assertEquals("Pixel (" + x + "," + y + ") is not transparent", 0, (argb >>> 24));
        }
      }
    } catch (Exception e) {
      Assert.fail("Did throw exception " + e.getMessage());
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
