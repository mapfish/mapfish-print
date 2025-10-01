package org.mapfish.print.url.data;

import static junit.framework.TestCase.assertEquals;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.Test;

public class TestDataUrlConnection {
  @Test
  public void testGetContentType() throws MalformedURLException, URISyntaxException {
    Handler.configureProtocolHandler();
    URL url = URL.of(new URI("data:image/svg+xml;base64,PHN2"), null);
    assertEquals("image/svg+xml", new DataUrlConnection(url).getContentType());
    URL url2 = URL.of(new URI("data:image/svg+xml,PHN2"), null);
    assertEquals("image/svg+xml", new DataUrlConnection(url2).getContentType());
    URL url3 = URL.of(new URI("data:,PHN2"), null);
    assertEquals("text/plain;charset=US-ASCII", new DataUrlConnection(url3).getContentType());
  }
}
