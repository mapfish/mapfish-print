package org.mapfish.print.url.data;

import static junit.framework.TestCase.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;

public class TestDataUrlConnection {
  @Test
  public void testGetContentType() throws MalformedURLException {
    Handler.configureProtocolHandler();
    assertEquals(
        "image/svg+xml",
        new DataUrlConnection(new URL("data:image/svg+xml;base64,PHN2")).getContentType());
    assertEquals(
        "image/svg+xml",
        new DataUrlConnection(new URL("data:image/svg+xml,PHN2")).getContentType());
    assertEquals(
        "text/plain;charset=US-ASCII",
        new DataUrlConnection(new URL("data:,PHN2")).getContentType());
  }
}
